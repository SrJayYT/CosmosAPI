package com.devpapo.cosmosapi.shop;

import com.cryptomorin.xseries.XMaterial;
import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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
    private final File menusFile;
    private final Map<String, FileConfiguration> shops = new HashMap<>();
    private final Map<String, FileConfiguration> sections = new HashMap<>();
    private final Map<String, FileConfiguration> inventories = new HashMap<>();
    private final Map<Inventory, OpenShop> openShops = new HashMap<>();
    private final Map<Inventory, QuantitySelection> quantityMenus = new HashMap<>();
    private FileConfiguration menusConfiguration;

    public CosmoShopManager(CosmosAPI plugin, CosmosService cosmosService) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
        File configDirectory = new File(plugin.getDataFolder(), "config");
        this.shopsDirectory = resolveDirectory(configDirectory, "shops");
        this.sectionsDirectory = resolveDirectory(configDirectory, "sections");
        this.inventoriesDirectory = resolveDirectory(configDirectory, "inventory");
        this.menusFile = resolveFile(configDirectory, "menus.yml");
        reload();
    }

    public void reload() {
        Map<Player, String> reopen = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            OpenShop openShop = openShops.get(player.getOpenInventory().getTopInventory());
            if (openShop != null) {
                reopen.put(player, openShop.id);
                player.closeInventory();
            }
        }
        openShops.clear();
        quantityMenus.clear();
        shopsDirectory.mkdirs();
        sectionsDirectory.mkdirs();
        inventoriesDirectory.mkdirs();
        loadDirectory(shopsDirectory, shops);
        loadDirectory(sectionsDirectory, sections);
        loadDirectory(inventoriesDirectory, inventories);
        menusConfiguration = YamlConfiguration.loadConfiguration(menusFile);
        if (!reopen.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> reopen.forEach((player, id) -> {
                if (player.isOnline()) {
                    openShop(player, id);
                }
            }));
        }
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

    public List<String> getInventoryIds() {
        List<String> ids = new ArrayList<>(inventories.keySet());
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public List<String> getShopStatus() {
        List<String> status = new ArrayList<>();
        for (String id : getShopIds()) {
            FileConfiguration section = sections.get(id);
            FileConfiguration shop = getShopForSection(id, section);
            if (shop == null) {
                continue;
            }
            List<String> issues = new ArrayList<>();
            if (!isEnabled(shop) || (section != null && !isEnabled(section))) {
                issues.add(plugin.getMessageManager().format("menu-status.disabled", Map.of()));
            }
            String inventoryId = section == null ? shop.getString("inventory", "default") : section.getString("inventory", shop.getString("inventory", "default"));
            if (!inventories.containsKey(inventoryId.toLowerCase(Locale.ROOT))) {
                issues.add("inventario inválido");
            }
            if (cosmosService.getCosmo(getCurrency(shop, section)) == null) {
                issues.add("cosmo inválido");
            }
            status.add(plugin.getMessageManager().format(issues.isEmpty() ? "menu-status.active" : "menu-status.with-issues", Map.of(
                "menu", id,
                "issues", String.join("&7, &e", issues)
            )));
        }
        return status;
    }

    public void openShopDirectory(Player player) {
        ConfigurationSection menu = menusConfiguration.getConfigurationSection("shops");
        if (menu == null) {
            send(player, "no-cosmo-shops", Map.of());
            return;
        }
        int size = getSize(menu.getInt("size", 45));
        String title = replace(player, menu.getString("title", "&8Tiendas Cosmos"), Map.of());
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.color(title));
        loadLayoutItems(player, inventory, menu);
        List<Integer> slots = getMenuSlots(menu.getIntegerList("shop-slots"), size);
        List<String> ids = getDirectoryShopIds();
        Map<Integer, Runnable> actions = new HashMap<>();
        Set<Integer> occupiedSlots = new HashSet<>();
        ConfigurationSection icon = menu.getConfigurationSection("shop-item");
        for (int index = 0; index < ids.size() && index < slots.size(); index++) {
            String id = ids.get(index);
            FileConfiguration section = sections.get(id);
            FileConfiguration shop = getShopForSection(id, section);
            if (shop == null) {
                continue;
            }
            String name = getShopName(shop, section, id);
            ItemStack item = section == null ? null : section.getItemStack("item");
            if (item == null) {
                item = createItem(player, section == null ? icon : section.getConfigurationSection("item"), Map.of("shop", name));
            } else {
                item = item.clone();
            }
            if (item != null) {
                int slot = section == null ? -1 : section.getInt("slot", -1);
                if (!slots.contains(slot) || occupiedSlots.contains(slot)) {
                    slot = -1;
                    for (int availableSlot : slots) {
                        if (!occupiedSlots.contains(availableSlot)) {
                            slot = availableSlot;
                            break;
                        }
                    }
                }
                if (slot < 0) {
                    continue;
                }
                inventory.setItem(slot, item);
                occupiedSlots.add(slot);
                actions.put(slot, () -> openShop(player, id));
            }
        }
        if (ids.isEmpty()) {
            ConfigurationSection empty = menu.getConfigurationSection("empty");
            int slot = getSlot(empty, size / 2, size);
            if (slot >= 0) {
                inventory.setItem(slot, createItem(player, empty, Map.of()));
            }
        }
        openShops.put(inventory, new OpenShop("directory", "", new HashMap<>(), actions));
        player.openInventory(inventory);
    }

    public boolean createShop(String id, String currency) {
        if (!isValidId(id) || shops.containsKey(id.toLowerCase(Locale.ROOT)) || !isActiveCosmo(currency)) {
            return false;
        }
        FileConfiguration shop = new YamlConfiguration();
        shop.set("enabled", true);
        shop.set("name", "&d&l" + id.toUpperCase(Locale.ROOT));
        shop.set("currency", currency);
        shop.createSection("items");
        if (!save(shop, new File(shopsDirectory, id.toLowerCase(Locale.ROOT) + ".yml"))) {
            return false;
        }
        reload();
        return true;
    }

    public boolean deleteShop(String id) {
        File file = getConfigurationFile(shopsDirectory, id);
        if (file == null || !file.delete()) {
            return false;
        }
        reload();
        return true;
    }

    public boolean setShopTitle(String id, String title) {
        FileConfiguration shop = shops.get(id.toLowerCase(Locale.ROOT));
        if (shop == null) {
            return false;
        }
        shop.set("name", title);
        return saveAndReload(shop, shopsDirectory, id);
    }

    public boolean setShopInventory(String id, String inventoryId) {
        FileConfiguration shop = shops.get(id.toLowerCase(Locale.ROOT));
        if (shop == null || !inventories.containsKey(inventoryId.toLowerCase(Locale.ROOT))) {
            return false;
        }
        shop.set("inventory", inventoryId.toLowerCase(Locale.ROOT));
        return saveAndReload(shop, shopsDirectory, id);
    }

    public boolean setShopItem(String id, int slot, ItemStack item, long buyPrice, long sellPrice) {
        FileConfiguration shop = shops.get(id.toLowerCase(Locale.ROOT));
        FileConfiguration layout = shop == null ? null : inventories.get(shop.getString("inventory", "default").toLowerCase(Locale.ROOT));
        if (layout == null) {
            layout = inventories.get("default");
        }
        if (shop == null || layout == null || item == null || item.getType().isAir() || !getProductSlots(layout, getSize(layout.getInt("size", 54))).contains(slot) || buyPrice < -1L || sellPrice < -1L || (buyPrice <= 0L && sellPrice <= 0L)) {
            return false;
        }
        String path = "items." + slot;
        shop.set(path + ".item", item.clone());
        shop.set(path + ".buy", buyPrice);
        shop.set(path + ".sell", sellPrice);
        return saveAndReload(shop, shopsDirectory, id);
    }

    public boolean removeShopItem(String id, int slot) {
        FileConfiguration shop = shops.get(id.toLowerCase(Locale.ROOT));
        if (shop == null || !shop.contains("items." + slot)) {
            return false;
        }
        shop.set("items." + slot, null);
        return saveAndReload(shop, shopsDirectory, id);
    }

    public boolean createInventory(String id) {
        if (!isValidId(id) || inventories.containsKey(id.toLowerCase(Locale.ROOT))) {
            return false;
        }
        FileConfiguration inventory = new YamlConfiguration();
        inventory.set("title", "&8" + id);
        inventory.set("size", 54);
        inventory.set("product-slots", defaultProductSlots(54));
        inventory.createSection("items");
        if (!save(inventory, new File(inventoriesDirectory, id.toLowerCase(Locale.ROOT) + ".yml"))) {
            return false;
        }
        reload();
        return true;
    }

    public boolean deleteInventory(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (key.equals("default")) {
            return false;
        }
        File file = getConfigurationFile(inventoriesDirectory, key);
        if (file == null || !file.delete()) {
            return false;
        }
        reload();
        return true;
    }

    public boolean setInventoryTitle(String id, String title) {
        FileConfiguration inventory = inventories.get(id.toLowerCase(Locale.ROOT));
        if (inventory == null) {
            return false;
        }
        inventory.set("title", title);
        return saveAndReload(inventory, inventoriesDirectory, id);
    }

    public boolean setInventorySize(String id, int size) {
        FileConfiguration inventory = inventories.get(id.toLowerCase(Locale.ROOT));
        if (inventory == null || getSize(size) != size) {
            return false;
        }
        inventory.set("size", size);
        return saveAndReload(inventory, inventoriesDirectory, id);
    }

    public boolean setInventoryItem(String id, int slot, ItemStack item) {
        FileConfiguration inventory = inventories.get(id.toLowerCase(Locale.ROOT));
        if (inventory == null || item == null || item.getType().isAir() || slot < 0 || slot >= getSize(inventory.getInt("size", 54))) {
            return false;
        }
        inventory.set("items." + slot + ".item", item.clone());
        return saveAndReload(inventory, inventoriesDirectory, id);
    }

    public boolean removeInventoryItem(String id, int slot) {
        FileConfiguration inventory = inventories.get(id.toLowerCase(Locale.ROOT));
        if (inventory == null || !inventory.contains("items." + slot)) {
            return false;
        }
        inventory.set("items." + slot, null);
        return saveAndReload(inventory, inventoriesDirectory, id);
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
            for (String configuredCommand : shop.getStringList("commands")) {
                String[] parts = configuredCommand.trim().toLowerCase(Locale.ROOT).split("\\s+");
                if (parts.length == 1 && parts[0].matches("[a-z0-9_-]{1,32}") && !commands.containsKey(parts[0])) {
                    commands.put(parts[0], id);
                }
            }
        }
        return commands;
    }

    public Map<String, Map<String, String>> getPublicShopSubcommands() {
        Map<String, Map<String, String>> commands = new LinkedHashMap<>();
        for (String id : getShopIds()) {
            FileConfiguration shop = shops.get(id);
            FileConfiguration section = sections.get(id);
            if (section != null) {
                String shopId = section.getString("shop", id).toLowerCase(Locale.ROOT);
                shop = shops.get(shopId);
            }
            if (shop == null || !isEnabled(shop) || (section != null && !isEnabled(section))) {
                continue;
            }
            for (String configuredCommand : shop.getStringList("commands")) {
                String[] parts = configuredCommand.trim().toLowerCase(Locale.ROOT).split("\\s+");
                if (parts.length != 2 || !parts[0].matches("[a-z0-9_-]{1,32}") || !parts[1].matches("[a-z0-9_-]{1,32}")) {
                    continue;
                }
                commands.computeIfAbsent(parts[0], ignored -> new LinkedHashMap<>()).putIfAbsent(parts[1], id);
            }
        }
        return commands;
    }

    public void openShop(Player player, String requestedId) {
        String id = requestedId.toLowerCase(Locale.ROOT);
        FileConfiguration shop = shops.get(id);
        FileConfiguration section = sections.get(id);
        if (shop == null && section != null) {
            shop = getShopForSection(id, section);
        }
        if (shop == null) {
            send(player, "no-menu", Map.of("menu", requestedId));
            return;
        }
        if (!isEnabled(shop) || (section != null && !isEnabled(section))) {
            send(player, "shop-disabled", Map.of());
            return;
        }
        if (!isActiveCosmo(getCurrency(shop, section))) {
            send(player, "shop-unknown-currency", Map.of());
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
            List<Integer> productSlots = getProductSlots(layout, size);
            Set<Integer> occupiedSlots = new HashSet<>();
            List<String> keys = new ArrayList<>(pageSection.getKeys(false));
            keys.sort(Comparator.comparing(String::toLowerCase));
            for (String key : keys) {
                ConfigurationSection item = pageSection.getConfigurationSection(key);
                Product product = item == null ? null : loadProduct(item);
                if (product == null) {
                    continue;
                }
                int slot = getProductSlot(key, item, productSlots, occupiedSlots, size);
                if (slot < 0) {
                    continue;
                }
                occupiedSlots.add(slot);
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
        openShops.put(inventory, new OpenShop(id, currency, products, actions));
        player.openInventory(inventory);
    }

    private List<String> getDirectoryShopIds() {
        List<String> ids = new ArrayList<>();
        for (String id : sections.keySet()) {
            FileConfiguration section = sections.get(id);
            FileConfiguration shop = getShopForSection(id, section);
            if (shop != null && isEnabled(section) && isEnabled(shop) && isActiveCosmo(getCurrency(shop, section))) {
                ids.add(id);
            }
        }
        for (String id : shops.keySet()) {
            if (!sections.containsKey(id) && isEnabled(shops.get(id)) && isActiveCosmo(getCurrency(shops.get(id), null))) {
                ids.add(id);
            }
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    private FileConfiguration getShopForSection(String sectionId, FileConfiguration section) {
        if (section == null) {
            return shops.get(sectionId);
        }
        return shops.get(section.getString("shop", sectionId).toLowerCase(Locale.ROOT));
    }

    public void handleClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        QuantitySelection quantitySelection = quantityMenus.get(top);
        if (quantitySelection != null) {
            event.setCancelled(true);
            if (top.equals(event.getClickedInventory()) && event.getWhoClicked() instanceof Player) {
                handleQuantityClick((Player) event.getWhoClicked(), top, quantitySelection, event.getSlot());
            }
            return;
        }
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
        openQuantityMenu((Player) event.getWhoClicked(), openShop.currency, product, !event.isRightClick());
    }

    public void handleDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!openShops.containsKey(top) && !quantityMenus.containsKey(top)) {
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
        quantityMenus.remove(inventory);
    }

    private void openQuantityMenu(Player player, String currency, Product product, boolean buying) {
        Inventory inventory = Bukkit.createInventory(null, 27, ColorUtil.color(buying ? "&8Comprar cantidad" : "&8Vender cantidad"));
        QuantitySelection selection = new QuantitySelection(currency, product, buying);
        quantityMenus.put(inventory, selection);
        renderQuantityMenu(inventory, selection);
        player.openInventory(inventory);
    }

    private void handleQuantityClick(Player player, Inventory inventory, QuantitySelection selection, int slot) {
        if (slot == 10) {
            selection.amount = Math.max(1, selection.amount - 1);
        } else if (slot == 11) {
            selection.amount = Math.min(selection.getMaximumAmount(), selection.amount + 1);
        } else if (slot == 13) {
            selection.amount = Math.max(1, selection.amount - selection.getStackSize());
        } else if (slot == 14) {
            selection.amount = Math.min(selection.getMaximumAmount(), selection.amount + selection.getStackSize());
        } else if (slot == 15) {
            selection.amount = selection.getMaximumAmount();
        } else if (slot == 16) {
            quantityMenus.remove(inventory);
            player.closeInventory();
            int amount = selection.getAmount();
            if (selection.buying) {
                buy(player, selection.currency, selection.product, amount);
            } else {
                sell(player, selection.currency, selection.product, amount);
            }
            return;
        } else if (slot == 17) {
            player.closeInventory();
            return;
        } else {
            return;
        }
        renderQuantityMenu(inventory, selection);
    }

    private void renderQuantityMenu(Inventory inventory, QuantitySelection selection) {
        inventory.clear();
        long price = calculatePrice(selection.buying ? selection.product.buyPrice : selection.product.sellPrice, selection.product, selection.getAmount());
        String operation = selection.buying ? "Comprar" : "Vender";
        ItemStack filler = quantityItem("GRAY_STAINED_GLASS_PANE", " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        int stackSize = selection.getStackSize();
        int stacks = (int) Math.ceil((double) selection.getAmount() / stackSize);
        inventory.setItem(10, quantityItem("REDSTONE", "&c&l− 1", List.of("&7Quitar una unidad", "&7Cantidad: &f" + selection.getAmount())));
        inventory.setItem(11, quantityItem("EMERALD", "&a&l+ 1", List.of("&7Añadir una unidad", "&7Máximo: &f" + selection.getMaximumAmount())));
        ItemStack product = selection.product.item.clone();
        product.setAmount(Math.min(selection.getAmount(), product.getMaxStackSize()));
        ItemMeta meta = product.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");
            lore.add("&f&lCantidad: &a" + selection.getAmount() + " &7ítem(s)");
            lore.add("&7Equivale a &f" + stacks + "&7/15 stack(s)");
            lore.add("&7Precio total: &e" + NumberFormatUtil.format(price));
            meta.setLore(ColorUtil.color(lore));
            product.setItemMeta(meta);
        }
        inventory.setItem(12, product);
        inventory.setItem(13, quantityItem("REDSTONE_BLOCK", "&c&l− 1 stack", List.of("&7Quitar &f" + stackSize + " &7ítem(s)")));
        inventory.setItem(14, quantityItem("CHEST", "&a&l+ 1 stack", List.of("&7Añadir &f" + stackSize + " &7ítem(s)", "&7Máximo: &f15 stacks")));
        inventory.setItem(15, quantityItem("PAPER", "&e&lMáximo", List.of("&7Seleccionar &f15 stacks", "&7Cantidad: &f" + selection.getMaximumAmount())));
        inventory.setItem(16, quantityItem("EMERALD", "&a&lConfirmar " + operation, List.of("&7Cantidad: &fx" + selection.getAmount(), "&7Precio total: &e" + NumberFormatUtil.format(price))));
        inventory.setItem(17, quantityItem("BARRIER", "&c&lCancelar", List.of("&7No se realizará ninguna operación")));
    }

    private ItemStack quantityItem(String material, String name, List<String> lore) {
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            meta.setLore(ColorUtil.color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void buy(Player player, String currency, Product product, int amount) {
        if (product.buyPrice <= 0L) {
            send(player, "shop-not-for-sale", Map.of());
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(currency);
        if (cosmo == null || !cosmo.isEnabled()) {
            send(player, "shop-unknown-currency", Map.of());
            return;
        }
        if (!hasInventorySpace(player, product.item, amount)) {
            send(player, "inventory-full", Map.of());
            return;
        }
        long price = calculatePrice(product.buyPrice, product, amount);
        if (!cosmosService.withdraw(player.getUniqueId(), cosmo.getId(), price)) {
            send(player, "insufficient-funds", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
            return;
        }
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = product.item.clone();
            stack.setAmount(Math.min(remaining, stack.getMaxStackSize()));
            player.getInventory().addItem(stack);
            remaining -= stack.getAmount();
        }
        send(player, "shop-purchase-success", transactionReplacements(product, cosmo, price, amount));
    }

    private void sell(Player player, String currency, Product product, int amount) {
        if (product.sellPrice <= 0L) {
            send(player, "shop-not-for-sell", Map.of());
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(currency);
        if (cosmo == null || !cosmo.isEnabled()) {
            send(player, "shop-unknown-currency", Map.of());
            return;
        }
        int available = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(product.item)) {
                available += stack.getAmount();
            }
        }
        if (available < amount) {
            send(player, "shop-sell-missing-items", Map.of());
            return;
        }
        int remaining = amount;
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
        long price = calculatePrice(product.sellPrice, product, amount);
        cosmosService.deposit(player.getUniqueId(), cosmo.getId(), price);
        send(player, "shop-sell-success", transactionReplacements(product, cosmo, price, amount));
    }

    private boolean hasInventorySpace(Player player, ItemStack item, int amount) {
        int capacity = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                capacity += item.getMaxStackSize();
            } else if (stack.isSimilar(item)) {
                capacity += Math.max(0, stack.getMaxStackSize() - stack.getAmount());
            }
            if (capacity >= amount) {
                return true;
            }
        }
        return false;
    }

    private long calculatePrice(long basePrice, Product product, int amount) {
        if (basePrice <= 0L || amount <= 0) {
            return basePrice;
        }
        double price = (double) basePrice * amount / product.item.getAmount();
        return price >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, (long) Math.ceil(price));
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

    private int getProductSlot(String key, ConfigurationSection item, List<Integer> productSlots, Set<Integer> occupiedSlots, int size) {
        int slot;
        try {
            slot = Integer.parseInt(key);
            return slot >= 0 && slot < size && productSlots.contains(slot) && !occupiedSlots.contains(slot) ? slot : -1;
        } catch (NumberFormatException ignored) {
        }
        if (item.contains("slot")) {
            slot = item.getInt("slot", -1);
            return slot >= 0 && slot < size && productSlots.contains(slot) && !occupiedSlots.contains(slot) ? slot : -1;
        }
        for (int productSlot : productSlots) {
            if (!occupiedSlots.contains(productSlot)) {
                return productSlot;
            }
        }
        return -1;
    }

    private boolean isEnabled(FileConfiguration configuration) {
        return configuration.contains("enabled") ? configuration.getBoolean("enabled") : configuration.getBoolean("enable", true);
    }

    private boolean isActiveCosmo(String currency) {
        CosmoDefinition cosmo = cosmosService.getCosmo(currency);
        return cosmo != null && cosmo.isEnabled();
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
        File[] files = directory.listFiles((current, name) -> {
            String lowerName = name.toLowerCase(Locale.ROOT);
            return lowerName.endsWith(".yml") || lowerName.endsWith(".yaml");
        });
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            destination.put(name.substring(0, name.lastIndexOf('.')).toLowerCase(Locale.ROOT), YamlConfiguration.loadConfiguration(file));
        }
    }

    private File resolveDirectory(File configDirectory, String name) {
        File configuredDirectory = new File(configDirectory, name);
        return configuredDirectory.isDirectory() ? configuredDirectory : new File(plugin.getDataFolder(), name);
    }

    private File resolveFile(File configDirectory, String name) {
        File configuredFile = new File(configDirectory, name);
        return configuredFile.isFile() ? configuredFile : new File(plugin.getDataFolder(), name);
    }

    private void loadLayoutItems(Player player, Inventory inventory, ConfigurationSection layout) {
        ConfigurationSection items = layout.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item != null && slot >= 0 && slot < inventory.getSize()) {
                    ItemStack layoutItem = item.getItemStack("item");
                    if (layoutItem == null) {
                        layoutItem = createItem(player, item, Map.of());
                    } else {
                        layoutItem = layoutItem.clone();
                    }
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

    private List<Integer> getProductSlots(FileConfiguration layout, int size) {
        List<Integer> slots = new ArrayList<>();
        for (int slot : layout.getIntegerList("product-slots")) {
            if (slot >= 0 && slot < size && !slots.contains(slot)) {
                slots.add(slot);
            }
        }
        if (!slots.isEmpty()) {
            return slots;
        }
        return defaultProductSlots(size);
    }

    private List<Integer> defaultProductSlots(int size) {
        List<Integer> slots = new ArrayList<>();
        int firstSlot = size >= 27 ? 9 : 0;
        int lastSlot = size >= 27 ? size - 10 : size - 1;
        for (int slot = firstSlot; slot <= lastSlot; slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private List<Integer> getMenuSlots(List<Integer> configuredSlots, int size) {
        List<Integer> slots = new ArrayList<>();
        for (int slot : configuredSlots) {
            if (slot >= 0 && slot < size && !slots.contains(slot)) {
                slots.add(slot);
            }
        }
        return slots.isEmpty() ? defaultProductSlots(size) : slots;
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

    private Map<String, String> transactionReplacements(Product product, CosmoDefinition cosmo, long price, int amount) {
        String name = product.item.hasItemMeta() && product.item.getItemMeta().hasDisplayName() ? product.item.getItemMeta().getDisplayName() : product.item.getType().name();
        return Map.of("amount", String.valueOf(amount), "item", name, "price", NumberFormatUtil.format(price), "cosmo", ColorUtil.color(cosmo.getDisplayName()));
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

    private boolean saveAndReload(FileConfiguration configuration, File directory, String id) {
        File file = getConfigurationFile(directory, id);
        if (file == null || !save(configuration, file)) {
            return false;
        }
        reload();
        return true;
    }

    private File getConfigurationFile(File directory, String id) {
        String key = id.toLowerCase(Locale.ROOT);
        for (String extension : List.of(".yml", ".yaml")) {
            File file = new File(directory, key + extension);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private boolean save(FileConfiguration configuration, File file) {
        try {
            configuration.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + file.getName(), exception);
            return false;
        }
    }

    private boolean isValidId(String id) {
        return id != null && id.matches("[A-Za-z0-9_-]{3,24}");
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

    private static final class QuantitySelection {
        private final String currency;
        private final Product product;
        private final boolean buying;
        private int amount = 1;

        private QuantitySelection(String currency, Product product, boolean buying) {
            this.currency = currency;
            this.product = product;
            this.buying = buying;
        }

        private int getAmount() {
            return amount;
        }

        private int getStackSize() {
            return Math.max(1, product.item.getMaxStackSize());
        }

        private int getMaximumAmount() {
            return getStackSize() * 15;
        }
    }

    private static final class OpenShop {
        private final String id;
        private final String currency;
        private final Map<Integer, Product> products;
        private final Map<Integer, Runnable> actions;

        private OpenShop(String id, String currency, Map<Integer, Product> products, Map<Integer, Runnable> actions) {
            this.id = id;
            this.currency = currency;
            this.products = products;
            this.actions = actions;
        }
    }
}