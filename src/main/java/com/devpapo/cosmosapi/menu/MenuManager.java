package com.devpapo.cosmosapi.menu;

import com.cryptomorin.xseries.XMaterial;
import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmosTrigger;
import com.devpapo.cosmosapi.storage.CosmosStorage;
import com.devpapo.cosmosapi.util.ColorUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public final class MenuManager {
    private final CosmosAPI plugin;
    private final CosmosStorage storage;
    private final CosmosService cosmosService;
    private final Map<Inventory, Map<Integer, MenuItem>> shops = new HashMap<>();
    private final Map<Inventory, Map<Integer, Runnable>> buttons = new HashMap<>();
    private final Map<Inventory, EditorSession> editors = new HashMap<>();
    private BukkitTask timeTask;

    public MenuManager(CosmosAPI plugin, CosmosStorage storage, CosmosService cosmosService) {
        this.plugin = plugin;
        this.storage = storage;
        this.cosmosService = cosmosService;
    }

    public void startTimeRewards() {
        timeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cosmosService.rewardTime(player.getUniqueId());
            }
        }, 1_200L, 1_200L);
    }

    public void stopTimeRewards() {
        if (timeTask != null) {
            timeTask.cancel();
            timeTask = null;
        }
    }

    public void restartTimeRewards() {
        stopTimeRewards();
        startTimeRewards();
    }

    public boolean createMenu(String id, String type, int size) {
        String key = id.toLowerCase(Locale.ROOT);
        if (storage.getMenuIds().contains(key)) {
            return false;
        }
        String path = "menus." + key;
        storage.getMenus().set(path + ".name", key);
        storage.getMenus().set(path + ".type", type.toUpperCase(Locale.ROOT));
        storage.getMenus().set(path + ".size", size);
        storage.getMenus().set(path + ".displayname", "&8" + id);
        storage.getMenus().set(path + ".command", "cosmos" + key);
        storage.getMenus().set(path + ".hidden", false);
        storage.getMenus().set(path + ".items", new HashMap<>());
        storage.saveMenus();
        return true;
    }

    public boolean deleteMenu(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!storage.getMenuIds().contains(key)) {
            return false;
        }
        storage.getMenus().set("menus." + key, null);
        storage.saveMenus();
        return true;
    }

    public boolean setDisplayName(String id, String displayName) {
        String key = id.toLowerCase(Locale.ROOT);
        if (!storage.getMenuIds().contains(key)) {
            return false;
        }
        storage.getMenus().set("menus." + key + ".displayname", displayName);
        storage.saveMenus();
        return true;
    }

    public ShopMenu getMenu(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        ConfigurationSection section = storage.getMenus().getConfigurationSection("menus." + key);
        if (section == null) {
            return null;
        }
        String type = section.getString("type", "CHEST").toUpperCase(Locale.ROOT);
        int defaultSize = "CHEST".equals(type) ? 27 : "DISPENSER".equals(type) ? 9 : ("BREWING".equals(type) || "HOPPER".equals(type)) ? 5 : 3;
        String name = section.getString("name", key);
        String displayName = section.getString("displayname", section.getString("title", "&8" + name));
        return new ShopMenu(key, name, type, section.getInt("size", defaultSize), displayName);
    }

    public List<String> getMenuIds() {
        List<String> ids = new ArrayList<>(storage.getMenuIds());
        ids.sort(String::compareToIgnoreCase);
        return ids;
    }

    public Map<String, String> getPublicMenuCommands() {
        Map<String, String> commands = new LinkedHashMap<>();
        for (String menuId : getMenuIds()) {
            String configured = storage.getMenus().getString("menus." + menuId + ".command", "");
            String command = configured == null ? "" : configured.trim().toLowerCase(Locale.ROOT);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (!command.matches("[a-z0-9_-]{1,32}")) {
                continue;
            }
            if (commands.containsKey(command)) {
                plugin.getLogger().warning("El comando /" + command + " está repetido en menus.yml; se usará el menú " + commands.get(command) + ".");
                continue;
            }
            commands.put(command, menuId);
        }
        return commands;
    }

    public List<String> getAvailableMenuIds() {
        List<String> available = new ArrayList<>();
        for (String menuId : getPublicMenuCommands().values()) {
            if (!storage.getMenus().getBoolean("menus." + menuId + ".hidden", false) && !available.contains(menuId)) {
                available.add(menuId);
            }
        }
        return available;
    }

    public void openMenuDirectory(Player player, int page) {
        List<String> menuIds = getAvailableMenuIds();
        ConfigurationSection directory = storage.getMenus().getConfigurationSection("menu-directory");
        int size = getChestSize(directory, 27);
        List<Integer> menuSlots = getSlots(directory, "menu-slots", defaultSlots(0, 21), size);
        int maxPage = Math.max(0, (menuIds.size() - 1) / Math.max(1, menuSlots.size()));
        int actualPage = Math.max(0, Math.min(page, maxPage));
        String title = directory == null ? "&8Menús disponibles" : directory.getString("title", "&8Menús disponibles");
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.color(replace(title, Map.of("page", String.valueOf(actualPage + 1)))));
        loadStaticItems(inventory, directory);
        Map<Integer, Runnable> actions = new HashMap<>();
        for (int index = 0; index < menuSlots.size() && actualPage * menuSlots.size() + index < menuIds.size(); index++) {
            String menuId = menuIds.get(actualPage * menuSlots.size() + index);
            ShopMenu menu = getMenu(menuId);
            if (menu == null) {
                continue;
            }
            int slot = menuSlots.get(index);
            inventory.setItem(slot, createConfiguredItem(directory == null ? null : directory.getConfigurationSection("menu-item"), "CHEST", 1, "{menu}", List.of("&7Clic para abrir este menú."), Map.of("menu", menu.getDisplayName())));
            actions.put(slot, () -> openShop(player, menuId));
        }
        if (menuIds.isEmpty()) {
            int emptySlot = getSlot(directory == null ? null : directory.getConfigurationSection("empty"), size / 2, size);
            inventory.setItem(emptySlot, createConfiguredItem(directory == null ? null : directory.getConfigurationSection("empty"), "BARRIER", 1, "&cNo hay menús disponibles.", List.of(), Map.of()));
        }
        ConfigurationSection previous = directory == null ? null : directory.getConfigurationSection("previous-page");
        int previousSlot = getSlot(previous, 21, size);
        if (actualPage > 0 && previousSlot >= 0) {
            inventory.setItem(previousSlot, createConfiguredItem(previous, "ARROW", 1, "&ePágina anterior", List.of(), Map.of("page", String.valueOf(actualPage))));
            actions.put(previousSlot, () -> openMenuDirectory(player, actualPage - 1));
        }
        ConfigurationSection next = directory == null ? null : directory.getConfigurationSection("next-page");
        int nextSlot = getSlot(next, 23, size);
        if (actualPage < maxPage && nextSlot >= 0) {
            inventory.setItem(nextSlot, createConfiguredItem(next, "ARROW", 1, "&ePágina siguiente", List.of(), Map.of("page", String.valueOf(actualPage + 2))));
            actions.put(nextSlot, () -> openMenuDirectory(player, actualPage + 1));
        }
        buttons.put(inventory, actions);
        player.openInventory(inventory);
    }

    public void openShop(Player player, String id) {
        openShop(player, id, 0);
    }

    public boolean openMenuEditor(Player player, String id) {
        ShopMenu menu = getMenu(id);
        if (menu == null) {
            return false;
        }
        Inventory inventory = createInventory(menu);
        Map<Integer, MenuItem> originalItems = loadItems(menu);
        for (Map.Entry<Integer, MenuItem> entry : originalItems.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < inventory.getSize() && !isShopNavigationSlot(menu, inventory.getSize(), slot)) {
                inventory.setItem(slot, entry.getValue().getItem().clone());
            }
        }
        if (hasShopNavigation(menu)) {
            ConfigurationSection navigation = storage.getMenus().getConfigurationSection("shop-navigation");
            int previousSlot = getShopPreviousSlot(menu, inventory.getSize());
            int backSlot = getShopBackSlot(menu, inventory.getSize());
            int nextSlot = getShopNextSlot(menu, inventory.getSize());
            ConfigurationSection previous = navigation == null ? null : navigation.getConfigurationSection("previous-page");
            ConfigurationSection next = navigation == null ? null : navigation.getConfigurationSection("next-page");
            if (next == null && navigation != null) {
                next = navigation.getConfigurationSection("page");
            }
            if (!isNineSlotChest(menu, inventory.getSize())) {
                inventory.setItem(previousSlot, createConfiguredItem(previous, "ARROW", 1, "&ePágina anterior", List.of("&7Slot reservado."), Map.of()));
            }
            inventory.setItem(backSlot, createConfiguredItem(navigation == null ? null : navigation.getConfigurationSection("back"), "GUNPOWDER", 1, "&eVolver", List.of("&7Slot reservado."), Map.of()));
            inventory.setItem(nextSlot, createConfiguredItem(next, "ARROW", 1, "&ePágina siguiente", List.of("&7Slot reservado."), Map.of()));
        }
        editors.put(inventory, new EditorSession(menu, originalItems));
        player.openInventory(inventory);
        return true;
    }

    private void openShop(Player player, String id, int page) {
        ShopMenu menu = getMenu(id);
        if (menu == null) {
            send(player, "no-menu", Map.of("menu", id));
            return;
        }
        Inventory inventory = createInventory(menu);
        Map<Integer, MenuItem> displayedItems = new HashMap<>();
        for (Map.Entry<Integer, MenuItem> entry : loadItems(menu).entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= inventory.getSize() || isShopNavigationSlot(menu, inventory.getSize(), slot)) {
                continue;
            }
            MenuItem item = entry.getValue();
            inventory.setItem(slot, item.getItem());
            if (item.getPrice() > 0L && cosmosService.getCosmo(item.getCosmoId()) != null) {
                displayedItems.put(slot, item);
            }
        }
        Map<Integer, Runnable> actions = new HashMap<>();
        if (hasShopNavigation(menu)) {
            ConfigurationSection navigation = storage.getMenus().getConfigurationSection("shop-navigation");
            int previousSlot = getShopPreviousSlot(menu, inventory.getSize());
            int backSlot = getShopBackSlot(menu, inventory.getSize());
            int nextSlot = getShopNextSlot(menu, inventory.getSize());
            ConfigurationSection previousSection = navigation == null ? null : navigation.getConfigurationSection("previous-page");
            ConfigurationSection nextSection = navigation == null ? null : navigation.getConfigurationSection("next-page");
            if (nextSection == null && navigation != null) {
                nextSection = navigation.getConfigurationSection("page");
            }
            if (!isNineSlotChest(menu, inventory.getSize())) {
                inventory.setItem(previousSlot, createConfiguredItem(previousSection, "ARROW", 1, "&ePágina anterior", List.of("&7No hay más páginas."), Map.of()));
            }
            inventory.setItem(backSlot, createConfiguredItem(navigation == null ? null : navigation.getConfigurationSection("back"), "GUNPOWDER", 1, "&eVolver", List.of("&7Volver al menú de cosmos."), Map.of()));
            inventory.setItem(nextSlot, createConfiguredItem(nextSection, "ARROW", 1, "&ePágina siguiente", List.of("&7No hay más páginas."), Map.of()));
            actions.put(backSlot, () -> openCosmosView(player, 0));
        }
        shops.put(inventory, displayedItems);
        buttons.put(inventory, actions);
        player.openInventory(inventory);
    }

    public boolean setItemSale(String menuId, int slot, String cosmoId, long price) {
        return setItemSale(null, menuId, slot, cosmoId, price);
    }

    public boolean setItemSale(Player player, String menuId, int slot, String cosmoId, long price) {
        ShopMenu menu = getMenu(menuId);
        Inventory inventory = menu == null ? null : createInventory(menu);
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        if (menu == null || inventory == null || slot < 0 || slot >= inventory.getSize() || isShopNavigationSlot(menu, inventory.getSize(), slot) || cosmo == null || price <= 0L) {
            return false;
        }
        String path = "menus." + menu.getId() + ".items." + slot;
        ItemStack item = player == null ? storage.getMenus().getItemStack(path + ".item") : player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return false;
        }
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("&8Precio: &f" + price + " " + cosmo.getDisplayName());
            meta.setLore(ColorUtil.color(lore));
            item.setItemMeta(meta);
        }
        storage.getMenus().set(path + ".item", item);
        storage.getMenus().set(path + ".cosmo", cosmoId);
        storage.getMenus().set(path + ".price", price);
        storage.saveMenus();
        return true;
    }

    public void openCosmosView(Player player, int page) {
        List<CosmoDefinition> cosmos = cosmosService.getCosmos();
        ConfigurationSection viewSection = storage.getMenus().getConfigurationSection("view");
        int size = getChestSize(viewSection, 27);
        List<Integer> cosmoSlots = getSlots(viewSection, "cosmo-slots", defaultSlots(0, 21), size);
        int maxPage = Math.max(0, (cosmos.size() - 1) / Math.max(1, cosmoSlots.size()));
        int actualPage = Math.max(0, Math.min(page, maxPage));
        String title = viewSection == null ? "&8Tus Cosmos" : viewSection.getString("title", "&8Tus Cosmos");
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.color(replace(title, Map.of("page", String.valueOf(actualPage + 1)))));
        loadStaticItems(inventory, viewSection);
        Map<Integer, Runnable> actions = new HashMap<>();
        for (int index = 0; index < cosmoSlots.size() && actualPage * cosmoSlots.size() + index < cosmos.size(); index++) {
            CosmoDefinition cosmo = cosmos.get(actualPage * cosmoSlots.size() + index);
            long balance = cosmosService.getBalance(player.getUniqueId(), cosmo.getId());
            ItemStack icon = createConfiguredItem(viewSection == null ? null : viewSection.getConfigurationSection("cosmo-item"), "NETHER_STAR", 1, "{cosmo}", List.of("&7Saldo: &f{balance}"), Map.of("cosmo", cosmo.getDisplayName(), "balance", String.valueOf(balance)));
            inventory.setItem(cosmoSlots.get(index), icon);
        }
        ConfigurationSection previousSection = viewSection == null ? null : viewSection.getConfigurationSection("previous-page");
        int previousSlot = getSlot(previousSection, 21, size);
        if (actualPage > 0 && previousSlot >= 0) {
            inventory.setItem(previousSlot, createConfiguredItem(previousSection, "ARROW", 1, "&ePágina anterior", List.of(), Map.of("page", String.valueOf(actualPage))));
            actions.put(previousSlot, () -> openCosmosView(player, actualPage - 1));
        }
        ConfigurationSection nextSection = viewSection == null ? null : viewSection.getConfigurationSection("next-page");
        int nextSlot = getSlot(nextSection, 23, size);
        if (actualPage < maxPage && nextSlot >= 0) {
            inventory.setItem(nextSlot, createConfiguredItem(nextSection, "ARROW", 1, "&ePágina siguiente", List.of(), Map.of("page", String.valueOf(actualPage + 2))));
            actions.put(nextSlot, () -> openCosmosView(player, actualPage + 1));
        }
        buttons.put(inventory, actions);
        player.openInventory(inventory);
    }

    public void openTopSelection(Player player) {
        ConfigurationSection selectionSection = storage.getMenus().getConfigurationSection("top-selection");
        int size = getChestSize(selectionSection, 27);
        List<Integer> slots = getSlots(selectionSection, "cosmo-slots", defaultSlots(0, size), size);
        String title = selectionSection == null ? "&8Top de Cosmos" : selectionSection.getString("title", "&8Top de Cosmos");
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.color(title));
        loadStaticItems(inventory, selectionSection);
        Map<Integer, Runnable> actions = new HashMap<>();
        for (int index = 0; index < slots.size() && index < cosmosService.getCosmos().size(); index++) {
            CosmoDefinition cosmo = cosmosService.getCosmos().get(index);
            int slot = slots.get(index);
            inventory.setItem(slot, createConfiguredItem(selectionSection == null ? null : selectionSection.getConfigurationSection("cosmo-item"), "NETHER_STAR", 1, "{cosmo}", List.of("&7Clic para ver el top 100."), Map.of("cosmo", cosmo.getDisplayName())));
            actions.put(slot, () -> openTop(player, cosmo, 0));
        }
        buttons.put(inventory, actions);
        player.openInventory(inventory);
    }

    private void openTop(Player player, CosmoDefinition cosmo, int page) {
        ConfigurationSection topSection = storage.getMenus().getConfigurationSection("top");
        int size = getChestSize(topSection, 27);
        List<Integer> slots = getSlots(topSection, "player-slots", defaultSlots(0, 15), size);
        int maxPage = Math.max(0, (100 - 1) / Math.max(1, slots.size()));
        int actualPage = Math.max(0, Math.min(page, maxPage));
        String title = topSection == null ? "&8Top: {cosmo}" : topSection.getString("title", "&8Top: {cosmo}");
        Inventory inventory = Bukkit.createInventory(null, size, ColorUtil.color(replace(title, Map.of("cosmo", cosmo.getDisplayName(), "page", String.valueOf(actualPage + 1)))));
        loadStaticItems(inventory, topSection);
        List<Map.Entry<UUID, Long>> topPlayers = cosmosService.getTop(cosmo.getId(), actualPage * slots.size(), slots.size());
        for (int index = 0; index < topPlayers.size(); index++) {
            Map.Entry<UUID, Long> entry = topPlayers.get(index);
            OfflinePlayer ranked = Bukkit.getOfflinePlayer(entry.getKey());
            String name = ranked.getName() == null ? entry.getKey().toString().substring(0, 8) : ranked.getName();
            inventory.setItem(slots.get(index), createConfiguredItem(topSection == null ? null : topSection.getConfigurationSection("player-item"), "PLAYER_HEAD", 1, "&d#{position} &f{player}", List.of("&7Saldo: &f{balance} {cosmo}"), Map.of("position", String.valueOf(actualPage * slots.size() + index + 1), "player", name, "balance", String.valueOf(entry.getValue()), "cosmo", cosmo.getDisplayName())));
        }
        Map<Integer, Runnable> actions = new HashMap<>();
        ConfigurationSection previousSection = topSection == null ? null : topSection.getConfigurationSection("previous-page");
        int previousSlot = getSlot(previousSection, 21, size);
        if (actualPage > 0 && previousSlot >= 0) {
            inventory.setItem(previousSlot, createConfiguredItem(previousSection, "ARROW", 1, "&ePágina anterior", List.of(), Map.of("page", String.valueOf(actualPage))));
            actions.put(previousSlot, () -> openTop(player, cosmo, actualPage - 1));
        }
        ConfigurationSection nextSection = topSection == null ? null : topSection.getConfigurationSection("next-page");
        int nextSlot = getSlot(nextSection, 23, size);
        if (actualPage < maxPage && nextSlot >= 0) {
            inventory.setItem(nextSlot, createConfiguredItem(nextSection, "ARROW", 1, "&ePágina siguiente", List.of(), Map.of("page", String.valueOf(actualPage + 2))));
            actions.put(nextSlot, () -> openTop(player, cosmo, actualPage + 1));
        }
        ConfigurationSection backSection = topSection == null ? null : topSection.getConfigurationSection("back");
        int backSlot = getSlot(backSection, 26, size);
        if (backSlot >= 0) {
            inventory.setItem(backSlot, createConfiguredItem(backSection, "GUNPOWDER", 1, "&eVolver", List.of("&7Volver a la selección de cosmos."), Map.of()));
            actions.put(backSlot, () -> openTopSelection(player));
        }
        buttons.put(inventory, actions);
        player.openInventory(inventory);
    }

    private int getChestSize(ConfigurationSection section, int defaultSize) {
        int size = section == null ? defaultSize : section.getInt("size", defaultSize);
        return size >= 9 && size <= 54 && size % 9 == 0 ? size : defaultSize;
    }

    private List<Integer> getSlots(ConfigurationSection section, String path, List<Integer> defaults, int inventorySize) {
        List<Integer> configured = section == null ? defaults : section.getIntegerList(path);
        List<Integer> slots = new ArrayList<>();
        for (int slot : configured) {
            if (slot >= 0 && slot < inventorySize && !slots.contains(slot)) {
                slots.add(slot);
            }
        }
        if (!slots.isEmpty() || section == null || !section.contains(path)) {
            return slots;
        }
        for (int slot : defaults) {
            if (slot >= 0 && slot < inventorySize && !slots.contains(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private List<Integer> defaultSlots(int start, int amount) {
        List<Integer> slots = new ArrayList<>();
        for (int index = 0; index < amount; index++) {
            slots.add(start + index);
        }
        return slots;
    }

    private int getSlot(ConfigurationSection section, int defaultSlot, int inventorySize) {
        int slot = section == null ? defaultSlot : section.getInt("slot", defaultSlot);
        return slot >= 0 && slot < inventorySize ? slot : -1;
    }

    private ItemStack createConfiguredItem(ConfigurationSection section, String defaultMaterial, int defaultAmount, String defaultName, List<String> defaultLore, Map<String, String> replacements) {
        String material = section == null ? defaultMaterial : section.getString("material", defaultMaterial);
        int amount = section == null ? defaultAmount : section.getInt("amount", defaultAmount);
        String name = section == null ? defaultName : section.getString("name", defaultName);
        List<String> lore = section == null ? defaultLore : section.getStringList("lore");
        return createItem(material, amount, replace(name, replacements), replace(lore, replacements));
    }

    private void loadStaticItems(Inventory inventory, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item != null && slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, createConfiguredItem(item, "STONE", 1, "&fItem", List.of(), Map.of()));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String replace(String value, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    private List<String> replace(List<String> values, Map<String, String> replacements) {
        List<String> replaced = new ArrayList<>();
        for (String value : values) {
            replaced.add(replace(value, replacements));
        }
        return replaced;
    }

    public void handleClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        EditorSession editor = editors.get(top);
        if (editor != null) {
            if (event.isShiftClick() || (event.getClickedInventory() != null && event.getClickedInventory().equals(top)
                    && isShopNavigationSlot(editor.menu, top.getSize(), event.getSlot()))) {
                event.setCancelled(true);
            }
            return;
        }
        Map<Integer, MenuItem> shopItems = shops.get(top);
        Map<Integer, Runnable> buttonActions = buttons.get(top);
        if (shopItems == null && buttonActions == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        int slot = event.getSlot();
        if (buttonActions != null) {
            Runnable action = buttonActions.get(slot);
            if (action != null) {
                action.run();
                return;
            }
        }
        MenuItem menuItem = shopItems.get(slot);
        if (menuItem != null && event.getWhoClicked() instanceof Player) {
            purchase((Player) event.getWhoClicked(), menuItem);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        EditorSession editor = editors.get(top);
        if (editor != null) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < top.getSize() && isShopNavigationSlot(editor.menu, top.getSize(), rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (!shops.containsKey(top) && !buttons.containsKey(top)) {
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
        EditorSession editor = editors.remove(inventory);
        if (editor == null) {
            shops.remove(inventory);
            buttons.remove(inventory);
            return;
        }
        Map<Integer, MenuItem> remainingOriginalItems = new HashMap<>(editor.originalItems);
        String itemsPath = "menus." + editor.menu.getId() + ".items";
        storage.getMenus().set(itemsPath, null);
        for (int slot : getShopProductSlots(editor.menu, inventory.getSize())) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            MenuItem original = takeMatchingOriginalItem(remainingOriginalItems, slot, item);
            String path = itemsPath + "." + slot;
            storage.getMenus().set(path + ".item", item.clone());
            storage.getMenus().set(path + ".cosmo", original == null ? "" : original.getCosmoId());
            storage.getMenus().set(path + ".price", original == null ? 0L : original.getPrice());
        }
        storage.saveMenus();
        shops.remove(inventory);
        buttons.remove(inventory);
    }

    private MenuItem takeMatchingOriginalItem(Map<Integer, MenuItem> originalItems, int slot, ItemStack item) {
        MenuItem sameSlot = originalItems.get(slot);
        if (sameSlot != null && sameSlot.getItem().isSimilar(item)) {
            originalItems.remove(slot);
            return sameSlot;
        }
        for (Map.Entry<Integer, MenuItem> entry : new ArrayList<>(originalItems.entrySet())) {
            if (entry.getValue().getItem().isSimilar(item)) {
                originalItems.remove(entry.getKey());
                return entry.getValue();
            }
        }
        return null;
    }

    private Inventory createInventory(ShopMenu menu) {
        if ("CHEST".equals(menu.getType())) {
            return Bukkit.createInventory(null, menu.getSize(), ColorUtil.color(menu.getDisplayName()));
        }
        try {
            return Bukkit.createInventory(null, InventoryType.valueOf(menu.getType()), ColorUtil.color(menu.getDisplayName()));
        } catch (IllegalArgumentException exception) {
            return Bukkit.createInventory(null, 27, ColorUtil.color(menu.getDisplayName()));
        }
    }

    private boolean hasShopNavigation(ShopMenu menu) {
        return "CHEST".equals(menu.getType()) || "DISPENSER".equals(menu.getType());
    }

    private int getShopPreviousSlot(ShopMenu menu, int inventorySize) {
        if ("DISPENSER".equals(menu.getType())) {
            return 6;
        }
        return inventorySize == 9 ? 0 : inventorySize - 9;
    }

    private int getShopBackSlot(ShopMenu menu, int inventorySize) {
        if ("DISPENSER".equals(menu.getType())) {
            return 7;
        }
        return inventorySize == 9 ? 0 : inventorySize - 5;
    }

    private int getShopNextSlot(ShopMenu menu, int inventorySize) {
        if ("DISPENSER".equals(menu.getType())) {
            return 8;
        }
        return inventorySize - 1;
    }

    private List<Integer> getShopProductSlots(ShopMenu menu, int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventorySize; slot++) {
            if (!hasShopNavigation(menu) || !isShopNavigationSlot(menu, inventorySize, slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private boolean isShopNavigationSlot(ShopMenu menu, int inventorySize, int slot) {
        if (!hasShopNavigation(menu)) {
            return false;
        }
        if ("CHEST".equals(menu.getType()) && inventorySize > 9) {
            return slot >= inventorySize - 9;
        }
        return slot == getShopPreviousSlot(menu, inventorySize)
            || slot == getShopBackSlot(menu, inventorySize)
            || slot == getShopNextSlot(menu, inventorySize);
    }

    private boolean isNineSlotChest(ShopMenu menu, int inventorySize) {
        return "CHEST".equals(menu.getType()) && inventorySize == 9;
    }

    public List<String> getMenuStatus() {
        List<String> status = new ArrayList<>();
        for (String menuId : getMenuIds()) {
            ShopMenu menu = getMenu(menuId);
            ConfigurationSection section = storage.getMenus().getConfigurationSection("menus." + menuId);
            if (menu == null || section == null) {
                continue;
            }
            List<String> issues = new ArrayList<>();
            if (!section.getBoolean("enabled", true)) {
                issues.add("deshabilitado");
            }
            if (section.getBoolean("hidden", false)) {
                issues.add("oculto");
            }
            String command = section.getString("command", "").trim();
            if (command.isEmpty()) {
                issues.add("sin comando público");
            }
            ConfigurationSection items = section.getConfigurationSection("items");
            int invalidItems = 0;
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ConfigurationSection item = items.getConfigurationSection(key);
                        ItemStack stack = item == null ? null : item.getItemStack("item");
                        if (item == null || slot < 0 || slot >= createInventory(menu).getSize() || isShopNavigationSlot(menu, createInventory(menu).getSize(), slot) || stack == null || stack.getType().isAir() || item.getLong("price", 0L) <= 0L || cosmosService.getCosmo(item.getString("cosmo", "")) == null) {
                            invalidItems++;
                        }
                    } catch (NumberFormatException exception) {
                        invalidItems++;
                    }
                }
            }
            if (invalidItems > 0) {
                issues.add(invalidItems + " ítem(s) sin precio, cosmo o slot válido");
            }
            status.add("&f" + menuId + "&7: " + (issues.isEmpty() ? "&aactivo y configurado" : "&e" + String.join("&7, &e", issues)));
        }
        return status;
    }

    private Map<Integer, MenuItem> loadItems(ShopMenu menu) {
        Map<Integer, MenuItem> items = new HashMap<>();
        ConfigurationSection section = storage.getMenus().getConfigurationSection("menus." + menu.getId() + ".items");
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ConfigurationSection itemSection = section.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                String cosmo = itemSection.getString("cosmo", "");
                long price = itemSection.getLong("price", 0L);
                ItemStack item = itemSection.getItemStack("item");
                if (item == null) {
                    item = createItem(itemSection.getString("material", "STONE"), itemSection.getInt("amount", 1), itemSection.getString("name", "&fItem"), itemSection.getStringList("lore"));
                }
                if (item != null) {
                    items.put(slot, new MenuItem(item, cosmo, price));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return items;
    }

    private void purchase(Player player, MenuItem menuItem) {
        CosmoDefinition cosmo = cosmosService.getCosmo(menuItem.getCosmoId());
        if (cosmo == null) {
            send(player, "unknown-cosmo", Map.of("cosmo", menuItem.getCosmoId()));
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            send(player, "inventory-full", Map.of());
            return;
        }
        if (!cosmosService.withdraw(player.getUniqueId(), cosmo.getId(), menuItem.getPrice())) {
            send(player, "insufficient-funds", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
            return;
        }
        player.getInventory().addItem(menuItem.getItem().clone());
        String itemName = menuItem.getItem().hasItemMeta() && menuItem.getItem().getItemMeta().hasDisplayName()
            ? menuItem.getItem().getItemMeta().getDisplayName() : menuItem.getItem().getType().name();
        send(player, "payment-success", Map.of("amount", String.valueOf(menuItem.getItem().getAmount()), "item", itemName, "price", String.valueOf(menuItem.getPrice()), "cosmo", ColorUtil.color(cosmo.getDisplayName())));
    }

    private ItemStack createItem(String materialName, int amount, String name, List<String> lore) {
        ItemStack item = XMaterial.matchXMaterial(materialName).map(XMaterial::parseItem).orElse(null);
        if (item == null) {
            return null;
        }
        item.setAmount(Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            meta.setLore(ColorUtil.color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void send(Player player, String key, Map<String, String> replacements) {
        String message = plugin.getConfig().getString("messages." + key, "");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") + message));
    }

    private static final class EditorSession {
        private final ShopMenu menu;
        private final Map<Integer, MenuItem> originalItems;

        private EditorSession(ShopMenu menu, Map<Integer, MenuItem> originalItems) {
            this.menu = menu;
            this.originalItems = new HashMap<>(originalItems);
        }
    }

}