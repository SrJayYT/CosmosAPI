package com.devpapo.cosmosapi.shop;

import com.cryptomorin.xseries.XMaterial;
import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CosmoShopManager {
    private final CosmosAPI plugin;
    private final CosmosService cosmosService;
    private final File shopsDirectory;
    private final File sectionsDirectory;
    private final File inventoriesDirectory;
    private final Map<String, FileConfiguration> shops = new HashMap<>();
    private final Map<String, FileConfiguration> sections = new HashMap<>();
    private final Map<String, FileConfiguration> inventories = new HashMap<>();
    private final Map<Inventory, OpenShop> openShops = new HashMap<>();

    public CosmoShopManager(CosmosAPI plugin, CosmosService cosmosService) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
        this.shopsDirectory = new File(plugin.getDataFolder(), "shops");
        this.sectionsDirectory = new File(plugin.getDataFolder(), "sections");
        this.inventoriesDirectory = new File(plugin.getDataFolder(), "inventory");
        reload();
    }

    public void reload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (openShops.containsKey(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
        }
        openShops.clear();
        shopsDirectory.mkdirs();
        sectionsDirectory.mkdirs();
        inventoriesDirectory.mkdirs();
        loadDirectory(shopsDirectory, shops);
        loadDirectory(sectionsDirectory, sections);
        loadDirectory(inventoriesDirectory, inventories);
    }

    public List<String> getShopIds() {
        List<String> ids = new ArrayList<>(shops.keySet());
        for (String sectionId : sections.keySet()) {
            if (!ids.contains(sectionId)) {
                ids.add(sectionId);
            }
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public Map<String, String> getPublicShopCommands() {
        Map<String, String> commands = new LinkedHashMap<>();
        for (String id : getShopIds()) {
            FileConfiguration shop = shops.get(id);
            FileConfiguration section = sections.get(id);
            if (section != null) {
                String shopId = section.getString("shop", id).toLowerCase(Locale.ROOT);
                shop = shops.get(shopId);
            }
            if (shop == null) {
                continue;
            }
            FileConfiguration commandSource = section == null ? shop : section;
            if (!isEnabled(shop) || !isEnabled(commandSource)) {
                continue;
            }
            String command = commandSource.getString("command", shop.getString("command", "")).trim().toLowerCase(Locale.ROOT);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (command.matches("[a-z0-9_-]{1,32}") && !commands.containsKey(command)) {
                commands.put(command, id);
            }
        }
        return commands;
    }

    public void openShop(Player player, String requestedId) {
        String id = requestedId.toLowerCase(Locale.ROOT);
        FileConfiguration shop = shops.get(id);
        FileConfiguration section = sections.get(id);
        if (shop == null && section != null) {
            String shopId = section.getString("shop", id).toLowerCase(Locale.ROOT);
            shop = shops.get(shopId);
        }
        if (shop == null) {
            send(player, "no-menu", Map.of("menu", requestedId));
            return;
        }
        if (!isEnabled(shop) || (section != null && !isEnabled(section))) {
            send(player, "shop-disabled", Map.of());
            return;
        }
        if (!hasPermission(player, shop) || (section != null && !hasPermission(player, section))) {
            send(player, "shop-no-permission", Map.of());
            return;
        }
        if (isDisabledWorld(player, shop) || (section != null && isDisabledWorld(player, section))) {
            send(player, "shop-disabled-world", Map.of());
            return;
        }
        String inventoryId = section == null ? shop.getString("inventory", "default") : section.getString("inventory", shop.getString("inventory", "default"));
        inventoryId = inventoryId.toLowerCase(Locale.ROOT);
        FileConfiguration layout = inventories.get(inventoryId);
        if (layout == null) {
            layout = inventories.get("default");
        }
        if (layout == null) {
            plugin.getLogger().warning("La tienda " + id + " no tiene un inventario válido.");
            return;
        }
        openPage(player, id, shop, section, layout, 0);
    }

    private void openPage(Player player, String id, FileConfiguration shop, FileConfiguration section, FileConfiguration layout, int requestedPage) {
        List<String> pages = getPages(shop);
        if (pages.isEmpty()) {
            plugin.getLogger().warning("La tienda " + id + " no tiene páginas configuradas.");
            return;
        }
        int page = Math.max(0, Math.min(requestedPage, pages.size() - 1));
        int size = getSize(layout.getInt("size", 54));
        String title = replace(player, layout.getString("title", "&8Tienda"), Map.of("shop", getShopName(shop, section, id), "page", String.valueOf(page + 1)));
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.color(title));
        loadLayoutItems(player, inventory, layout);
        String currency = getCurrency(shop, section);
        CosmoDefinition cosmo = cosmosService.getCosmo(currency);
        Map<Integer, Product> products = new HashMap<>();
        ConfigurationSection pageSection = getPageItems(shop, pages.get(page));
        if (pageSection != null) {
            int automaticSlot = size >= 27 ? 9 : 0;
            for (String key : pageSection.getKeys(false)) {
                ConfigurationSection item = pageSection.getConfigurationSection(key);
                Product product = item == null ? null : loadProduct(item);
                if (product == null) {
                    continue;
                }
                int slot = getProductSlot(key, item, automaticSlot, size);
                if (slot < 0 || slot >= size) {
                    continue;
                }
                automaticSlot = slot + 1;
                inventory.setItem(slot, displayProduct(player, product, cosmo));
                products.put(slot, product);
            }
        }
        Map<Integer, Runnable> actions = new HashMap<>();
        ConfigurationSection previous = layout.getConfigurationSection("previous-page");
        int previousSlot = getSlot(previous, -1, size);
        if (page > 0 && previousSlot >= 0) {
            inventory.setItem(previousSlot, createItem(player, previous, Map.of("page", String.valueOf(page))));
            int previousPage = page - 1;
            actions.put(previousSlot, () -> openPage(player, id, shop, section, layout, previousPage));
        }
        ConfigurationSection next = layout.getConfigurationSection("next-page");
        int nextSlot = getSlot(next, -1, size);
        if (page < pages.size() - 1 && nextSlot >= 0) {
            inventory.setItem(nextSlot, createItem(player, next, Map.of("page", String.valueOf(page + 2))));
            int nextPage = page + 1;
            actions.put(nextSlot, () -> openPage(player, id, shop, section, layout, nextPage));
        }
        ConfigurationSection balance = layout.getConfigurationSection("balance-item");
        int balanceSlot = getSlot(balance, -1, size);
        if (balanceSlot >= 0 && cosmo != null) {
            inventory.setItem(balanceSlot, createItem(player, balance, replacements(cosmo, cosmosService.getBalance(player.getUniqueId(), cosmo.getId()), Map.of())));
        }
        openShops.put(inventory, new OpenShop(currency, products, actions));
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        OpenShop openShop = openShops.get(top);
        if (openShop == null) {
            return;
        }
        event.setCancelled(true);
        if (!top.equals(event.getClickedInventory()) || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Runnable action = openShop.actions.get(event.getSlot());
        if (action != null) {
            action.run();
            return;
        }
        Product product = openShop.products.get(event.getSlot());
        if (product == null) {
            return;
        }
        if (event.isRightClick()) {
            sell((Player) event.getWhoClicked(), openShop.currency, product);
        } else {
            buy((Player) event.getWhoClicked(), openShop.currency, product);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!openShops.containsKey(top)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void handleClose(Inventory inventory) {
        openShops.remove(inventory);
    }

    private void buy(Player player, String currency, Product product) {
        if (product.buyPrice <= 0L) {
            send(player, "shop-not-for-sale", Map.of());
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(currency);
        if (cosmo == null || !cosmo.isEnabled()) {
            send(player, "shop-unknown-currency", Map.of());
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            send(player, "inventory-full", Map.of());
            return;
        }
        if (!cosmosService.withdraw(player.getUniqueId(), cosmo.getId(), product.buyPrice)) {
            send(player, "insufficient-funds", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
            return;
        }
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(product.item.clone());
        for (ItemStack leftover : remaining.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        send(player, "shop-purchase-success", transactionReplacements(product, cosmo, product.buyPrice));
    }

    private void sell(Player player, String currency, Product product) {
        if (product.sellPrice <= 0L) {
            send(player, "shop-not-for-sell", Map.of());
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(currency);
        if (cosmo == null || !cosmo.isEnabled()) {
            send(player, "shop-unknown-currency", Map.of());
            return;
        }
        int remaining = product.item.getAmount();
        for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || !stack.isSimilar(product.item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - removed);
            remaining -= removed;
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(slot, null);
            }
        }
        if (remaining > 0) {
            restoreSoldItems(player, product.item, product.item.getAmount() - remaining);
            send(player, "shop-sell-missing-items", Map.of());
            return;
        }
        cosmosService.deposit(player.getUniqueId(), cosmo.getId(), product.sellPrice);
        send(player, "shop-sell-success", transactionReplacements(product, cosmo, product.sellPrice));
    }

    private void restoreSoldItems(Player player, ItemStack item, int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack restored = item.clone();
        restored.setAmount(amount);
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(restored);
        for (ItemStack leftover : remaining.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private Product loadProduct(ConfigurationSection section) {
        ItemStack item = section.getItemStack("item");
        if (item == null) {
            item = createItem(null, section, Map.of());
        }
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return new Product(item, section.getLong("buy", -1L), section.getLong("sell", -1L));
    }

    private ItemStack displayProduct(Player player, Product product, CosmoDefinition cosmo) {
        ItemStack display = product.item.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(" ");
        lore.add(product.buyPrice > 0L ? "&a▶ Click izquierdo: &fComprar por " + NumberFormatUtil.format(product.buyPrice) : "&c▶ No se puede comprar");
        lore.add(product.sellPrice > 0L ? "&e▶ Click derecho: &fVender por " + NumberFormatUtil.format(product.sellPrice) : "&c▶ No se puede vender");
        if (cosmo != null) {
            lore.add("&7Moneda: " + cosmo.getDisplayName());
        }
        meta.setLore(ColorUtil.color(replacePlaceholders(player, lore)));
        display.setItemMeta(meta);
        return display;
    }

    private List<String> getPages(FileConfiguration shop) {
        ConfigurationSection section = shop.getConfigurationSection("pages");
        if (section != null) {
            List<String> pages = new ArrayList<>(section.getKeys(false));
            pages.sort(Comparator.comparing(String::toLowerCase));
            return pages;
        }
        return List.of(shop.isConfigurationSection("items") ? "items" : "root");
    }

    private ConfigurationSection getPageItems(FileConfiguration shop, String page) {
        if (shop.isConfigurationSection("pages." + page + ".items")) {
            return shop.getConfigurationSection("pages." + page + ".items");
        }
        if ("items".equals(page) && shop.isConfigurationSection("items")) {
            return shop.getConfigurationSection("items");
        }
        return "root".equals(page) ? shop : null;
    }

    private int getProductSlot(String key, ConfigurationSection item, int automaticSlot, int size) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
        }
        if (item.contains("slot")) {
            return item.getInt("slot", -1);
        }
        int lastProductSlot = size >= 27 ? size - 10 : size - 1;
        return automaticSlot <= lastProductSlot ? automaticSlot : -1;
    }

    private boolean isEnabled(FileConfiguration configuration) {
        return configuration.contains("enabled") ? configuration.getBoolean("enabled") : configuration.getBoolean("enable", true);
    }

    private boolean hasPermission(Player player, FileConfiguration configuration) {
        String permission = configuration.getString("permission", "").trim();
        return permission.isEmpty() || player.hasPermission(permission);
    }

    private boolean isDisabledWorld(Player player, FileConfiguration configuration) {
        for (String world : configuration.getStringList("disabled-worlds")) {
            if (player.getWorld().getName().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    private String getShopName(FileConfiguration shop, FileConfiguration section, String id) {
        String name = shop.getString("name", shop.getString("title", ""));
        if (!name.isEmpty()) {
            return name;
        }
        return section == null ? id : section.getString("title", id);
    }

    private String getCurrency(FileConfiguration shop, FileConfiguration section) {
        String currency = shop.getString("currency", "").trim();
        if (currency.isEmpty() && section != null) {
            currency = section.getString("currency", "").trim();
        }
        if (!currency.isEmpty()) {
            return currency;
        }
        String economy = section == null ? shop.getString("economy", "") : section.getString("economy", shop.getString("economy", ""));
        String[] parts = economy.split(":", 3);
        if (parts.length == 3 && parts[0].equalsIgnoreCase("EXTERNAL") && parts[1].equalsIgnoreCase("CosmosAPI")) {
            return parts[2];
        }
        return "";
    }

    private void loadDirectory(File directory, Map<String, FileConfiguration> destination) {
        destination.clear();
        File[] files = directory.listFiles((current, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            destination.put(name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT), YamlConfiguration.loadConfiguration(file));
        }
    }

    private void loadLayoutItems(Player player, Inventory inventory, FileConfiguration layout) {
        ConfigurationSection items = layout.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item != null && slot >= 0 && slot < inventory.getSize()) {
                    ItemStack layoutItem = createItem(player, item, Map.of());
                    if (layoutItem != null) {
                        inventory.setItem(slot, layoutItem);
                    } else {
                        plugin.getLogger().warning("Material inválido en inventory/" + layout.getName() + ".yml, slot " + slot + ".");
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private ItemStack createItem(Player player, ConfigurationSection section, Map<String, String> replacements) {
        String material = section.getString("material", "STONE");
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        item.setAmount(Math.max(1, Math.min(64, section.getInt("amount", 1))));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(replace(player, section.getString("name", "&fItem"), replacements)));
            meta.setLore(ColorUtil.color(replacePlaceholders(player, replace(section.getStringList("lore"), replacements))));
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getSize(int size) {
        return size >= 9 && size <= 54 && size % 9 == 0 ? size : 54;
    }

    private int getSlot(ConfigurationSection section, int fallback, int size) {
        if (section == null) {
            return -1;
        }
        int slot = section.getInt("slot", fallback);
        return slot >= 0 && slot < size ? slot : -1;
    }

    private Map<String, String> replacements(CosmoDefinition cosmo, long balance, Map<String, String> extra) {
        Map<String, String> values = new HashMap<>(extra);
        values.put("cosmo", cosmo.getDisplayName());
        values.put("cosmo-name", cosmo.getDisplayName());
        values.put("balance", NumberFormatUtil.format(balance));
        return values;
    }

    private Map<String, String> transactionReplacements(Product product, CosmoDefinition cosmo, long price) {
        String name = product.item.hasItemMeta() && product.item.getItemMeta().hasDisplayName() ? product.item.getItemMeta().getDisplayName() : product.item.getType().name();
        return Map.of("amount", String.valueOf(product.item.getAmount()), "item", name, "price", NumberFormatUtil.format(price), "cosmo", ColorUtil.color(cosmo.getDisplayName()));
    }

    private String replace(Player player, String value, Map<String, String> replacements) {
        String replaced = value;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return replacePlaceholders(player, replaced);
    }

    private List<String> replace(List<String> values, Map<String, String> replacements) {
        List<String> replaced = new ArrayList<>();
        for (String value : values) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                value = value.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            replaced.add(value);
        }
        return replaced;
    }

    private String replacePlaceholders(Player player, String value) {
        if (player == null || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return value;
        }
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object result = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class).invoke(null, player, value);
            return result instanceof String ? (String) result : value;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return value;
        }
    }

    private List<String> replacePlaceholders(Player player, List<String> values) {
        List<String> replaced = new ArrayList<>();
        for (String value : values) {
            replaced.add(replacePlaceholders(player, value));
        }
        return replaced;
    }

    private void send(Player player, String key, Map<String, String> replacements) {
        plugin.getMessageManager().send(player, key, replacements);
    }

    private static final class Product {
        private final ItemStack item;
        private final long buyPrice;
        private final long sellPrice;

        private Product(ItemStack item, long buyPrice, long sellPrice) {
            this.item = item;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }

    private static final class OpenShop {
        private final String currency;
        private final Map<Integer, Product> products;
        private final Map<Integer, Runnable> actions;

        private OpenShop(String currency, Map<Integer, Product> products, Map<Integer, Runnable> actions) {
            this.currency = currency;
            this.products = products;
            this.actions = actions;
        }
    }
}