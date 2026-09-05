package com.devpapo.cosmosapi.command;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmosTrigger;
import com.devpapo.cosmosapi.cosmo.TimeRewardUnit;
import com.devpapo.cosmosapi.hologram.HologramManager;
import com.devpapo.cosmosapi.menu.MenuManager;
import com.devpapo.cosmosapi.shop.CosmoShopManager;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import com.cryptomorin.xseries.XSound;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CosmoCommand implements CommandExecutor, TabCompleter {
    private final CosmosAPI plugin;
    private final CosmosService cosmosService;
    private final MenuManager menuManager;
    private final CosmoShopManager cosmoShopManager;
    private final HologramManager hologramManager;

    public CosmoCommand(CosmosAPI plugin, CosmosService cosmosService, MenuManager menuManager, CosmoShopManager cosmoShopManager, HologramManager hologramManager) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
        this.menuManager = menuManager;
        this.cosmoShopManager = cosmoShopManager;
        this.hologramManager = hologramManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "create":
                createCosmo(sender, args);
                return true;
            case "edit":
                editCosmo(sender, args);
                return true;
            case "pars":
                previewIcon(sender, args);
                return true;
            case "displayname":
                displayName(sender, args);
                return true;
            case "delete":
                delete(sender, args);
                return true;
            case "menu":
                menu(sender, args);
                return true;
            case "menus":
                menus(sender, args);
                return true;
            case "shop":
            case "shops":
                shop(sender, args);
                return true;
            case "inventory":
            case "inventories":
                inventory(sender, args);
                return true;
            case "hologram":
            case "holograms":
                hologram(sender, args);
                return true;
            case "reload":
                reload(sender);
                return true;
            case "view":
                view(sender);
                return true;
            case "list":
                list(sender);
                return true;
            case "status":
                status(sender, args);
                return true;
            case "tops":
                tops(sender);
                return true;
            case "balance":
                balance(sender, args);
                return true;
            case "baltop":
                baltop(sender, args);
                return true;
            case "send":
                sendCosmo(sender, args);
                return true;
            case "give":
                changeBalance(sender, args, false);
                return true;
            case "take":
                takeBalance(sender, args);
                return true;
            case "giveall":
                giveAll(sender, args);
                return true;
            case "takeall":
                takeAll(sender, args);
                return true;
            case "set":
                changeBalance(sender, args, true);
                return true;
            default:
                help(sender);
                return true;
        }
    }

    private void createCosmo(CommandSender sender, String[] args) {
        if (!admin(sender) || (args.length != 4 && args.length != 6)) {
            usage(sender, "/cosmo create <nombre> <tipo> <cantidad> [intervalo] [minuto|dia|semana|mes|año]");
            return;
        }
        if (!validId(args[1])) {
            message(sender, "invalid-name", Map.of());
            return;
        }
        CosmosTrigger trigger = CosmosTrigger.fromInput(args[2]);
        long reward = amount(sender, args[3]);
        if (trigger == null || reward <= 0L || (trigger != CosmosTrigger.TIME && args.length != 4)) {
            message(sender, "invalid-trigger-types", Map.of("types", Arrays.toString(CosmosTrigger.values())));
            return;
        }
        long interval = 1L;
        TimeRewardUnit unit = TimeRewardUnit.MINUTE;
        if (trigger == CosmosTrigger.TIME && args.length == 6) {
            interval = amount(sender, args[4]);
            unit = TimeRewardUnit.fromInput(args[5]);
            if (interval <= 0L || unit == null) {
                usage(sender, "/cosmo create <nombre> TIME <cantidad> <intervalo> <minuto|dia|semana|mes|año>");
                return;
            }
        }
        if (!cosmosService.create(args[1], trigger, reward, interval, unit)) {
            message(sender, "cosmo-already-exists", Map.of());
            return;
        }
        String time = trigger == CosmosTrigger.TIME ? plugin.getMessageManager().format("time-reward-suffix", Map.of("interval", String.valueOf(interval), "unit", unit.name())) : "";
        message(sender, "cosmo-created", Map.of("cosmo", args[1], "reward", NumberFormatUtil.format(reward), "trigger", trigger.name(), "time", time));
    }

    private void displayName(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length < 3) {
            usage(sender, "/cosmo displayname <cosmo> <nombre con color>");
            return;
        }
        if (!cosmosService.setDisplayName(args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)))) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        message(sender, "display-name-updated", Map.of());
    }

    private void previewIcon(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 3 || !args[1].equalsIgnoreCase("set")) {
            usage(sender, "/cosmo pars set <cosmo>");
            return;
        }
        if (!(sender instanceof Player)) {
            message(sender, "player-only", Map.of());
            return;
        }
        Player player = (Player) sender;
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            message(sender, "preview-icon-invalid", Map.of());
            return;
        }
        if (!cosmosService.setPreviewIcon(args[2], player.getInventory().getItemInMainHand())) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[2]));
            return;
        }
        message(sender, "preview-icon-saved", Map.of("cosmo", args[2]));
    }

    private void editCosmo(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length < 4) {
            usage(sender, "/cosmo edit <cosmo> <displayname|type|reward|interval> <valor>");
            return;
        }
        String field = args[2].toLowerCase(Locale.ROOT);
        boolean updated;
        if (field.equals("displayname")) {
            updated = cosmosService.setDisplayName(args[1], String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
        } else if (field.equals("type") && args.length == 4) {
            updated = cosmosService.setTrigger(args[1], CosmosTrigger.fromInput(args[3]));
        } else if (field.equals("reward") && args.length == 4) {
            updated = cosmosService.setReward(args[1], amount(sender, args[3]));
        } else if (field.equals("interval") && args.length == 5) {
            updated = cosmosService.setTimeInterval(args[1], amount(sender, args[3]), TimeRewardUnit.fromInput(args[4]));
        } else {
            usage(sender, "/cosmo edit <cosmo> <displayname|type|reward|interval> <valor>");
            return;
        }
        if (updated) {
            message(sender, "cosmo-updated", Map.of());
        } else {
            message(sender, "cosmo-update-failed", Map.of());
        }
    }

    private void delete(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 2) {
            usage(sender, "/cosmo delete <cosmo>");
            return;
        }
        if (cosmosService.delete(args[1])) {
            hologramManager.deleteForCosmo(args[1]);
            message(sender, "cosmo-deleted", Map.of());
        } else {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
        }
    }

    private void menu(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/cosmo menu <list|create|open|edit|item|displayname|enabled|disabled|status|delete> ...");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("enabled") || action.equals("disabled")) {
            if (!admin(sender) || args.length != 3) {
                usage(sender, "/cosmo menu " + action + " <nombre>");
                return;
            }
            boolean enabled = action.equals("enabled");
            if (!menuManager.setMenuEnabled(args[2], enabled)) {
                message(sender, "no-menu", Map.of("menu", args[2]));
                return;
            }
            plugin.refreshMenuCommands();
            message(sender, enabled ? "menu-enabled" : "menu-disabled", Map.of("menu", args[2]));
            return;
        }
        if (!plugin.areMenusEnabled()) {
            message(sender, "menus-disabled", Map.of());
            return;
        }
        if (action.equals("status")) {
            if (!admin(sender) || args.length != 2) {
                usage(sender, "/cosmo menu status");
                return;
            }
            List<String> status = cosmoShopManager.getShopStatus();
            message(sender, status.isEmpty() ? "no-menus" : "menu-status-header", Map.of());
            for (String line : status) {
                sender.sendMessage(ColorUtil.color(line));
            }
            return;
        }
        if (action.equals("list")) {
            if (!(sender instanceof Player) || args.length != 2) {
                usage(sender, "/cosmo menu list");
                return;
            }
            cosmoShopManager.openShopDirectory((Player) sender);
            return;
        }
        if (action.equals("create")) {
            if (!admin(sender) || args.length != 5) {
                usage(sender, "/cosmo menu create <nombre> <cofre|dispensador|horno|soporte|tolva> <tamaño>");
                return;
            }
            if (!validId(args[2])) {
                message(sender, "invalid-name", Map.of());
                return;
            }
            String type = normalizeMenuType(args[3]);
            int size;
            try {
                size = Integer.parseInt(args[4]);
            } catch (NumberFormatException exception) {
                message(sender, "invalid-number", Map.of());
                return;
            }
            if (type == null || !validMenuSize(type, size)) {
                message(sender, "invalid-menu-size", Map.of());
                return;
            }
            if (!menuManager.createMenu(args[2], type, size)) {
                message(sender, "menu-already-exists", Map.of());
                return;
            }
            message(sender, "menu-created", Map.of("menu", args[2]));
            return;
        }
        if (action.equals("open")) {
            if (!(sender instanceof Player)) {
                message(sender, "player-only", Map.of());
                return;
            }
            if (args.length != 3) {
                usage(sender, "/cosmo menu open <nombre>");
                return;
            }
            menuManager.openShop((Player) sender, args[2]);
            return;
        }
        if (action.equals("edit")) {
            if (!admin(sender) || !(sender instanceof Player) || args.length != 3) {
                usage(sender, "/cosmo menu edit <nombre>");
                return;
            }
            if (!menuManager.openMenuEditor((Player) sender, args[2])) {
                message(sender, "no-menu", Map.of("menu", args[2]));
            }
            return;
        }
        if (action.equals("item")) {
            if (!admin(sender) || args.length != 6) {
                usage(sender, "/cosmo menu item <menú> <slot> <cosmo> <precio>");
                return;
            }
            int slot;
            try {
                slot = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                message(sender, "invalid-number", Map.of());
                return;
            }
            long price = amount(sender, args[5]);
            Player player = sender instanceof Player ? (Player) sender : null;
            if (price <= 0L || !menuManager.setItemSale(player, args[2], slot, args[4], price)) {
                message(sender, "invalid-menu-item", Map.of());
                return;
            }
            message(sender, "menu-item-updated", Map.of());
            return;
        }
        if (action.equals("displayname")) {
            if (!admin(sender) || args.length < 4) {
                usage(sender, "/cosmo menu displayname <menú> <nombre>");
                return;
            }
            message(sender, menuManager.setDisplayName(args[2], String.join(" ", Arrays.copyOfRange(args, 3, args.length))) ? "menu-display-name-updated" : "no-menu", Map.of("menu", args[2]));
            return;
        }
        if (action.equals("delete")) {
            if (!admin(sender) || args.length != 3) {
                usage(sender, "/cosmo menu delete <nombre>");
                return;
            }
            message(sender, menuManager.deleteMenu(args[2]) ? "menu-deleted" : "no-menu", Map.of("menu", args[2]));
            return;
        }
        usage(sender, "/cosmo menu <list|create|open|edit|item|displayname|enabled|disabled|status|delete> ...");
    }

    private void menus(CommandSender sender, String[] args) {
        if (!plugin.areMenusEnabled()) {
            message(sender, "menus-disabled", Map.of());
            return;
        }
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!(sender instanceof Player) || args.length > 2) {
            usage(sender, "/cosmo menus [página]");
            return;
        }
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                message(sender, "invalid-number", Map.of());
                return;
            }
        }
        cosmoShopManager.openShopDirectory((Player) sender);
    }

    private void shop(CommandSender sender, String[] args) {
        if (args.length >= 2 && isShopAdminAction(args[1])) {
            editShop(sender, args);
            return;
        }
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!(sender instanceof Player)) {
            message(sender, "player-only", Map.of());
            return;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("menu")) {
            cosmoShopManager.openShopDirectory((Player) sender);
            return;
        }
        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            List<String> shops = cosmoShopManager.getShopIds();
            if (shops.isEmpty()) {
                message(sender, "no-cosmo-shops", Map.of());
            } else {
                message(sender, "cosmo-shops", Map.of("shops", String.join("&7, &f", shops)));
            }
            return;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("open")) {
            cosmoShopManager.openShop((Player) sender, args[2]);
            return;
        }
        usage(sender, "/cosmo shop [menu|list|open <tienda>]");
    }

    private void editShop(CommandSender sender, String[] args) {
        if (!admin(sender)) {
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("reload") && args.length == 2) {
            cosmoShopManager.reload();
            plugin.refreshMenuCommands();
            message(sender, "shops-reloaded", Map.of());
            return;
        }
        if (action.equals("create") && args.length == 4) {
            message(sender, cosmoShopManager.createShop(args[2], args[3]) ? "shop-created" : "shop-save-failed", Map.of("shop", args[2]));
            plugin.refreshMenuCommands();
            return;
        }
        if (action.equals("delete") && args.length == 3) {
            message(sender, cosmoShopManager.deleteShop(args[2]) ? "shop-deleted" : "shop-save-failed", Map.of("shop", args[2]));
            plugin.refreshMenuCommands();
            return;
        }
        if (action.equals("title") && args.length >= 4) {
            message(sender, cosmoShopManager.setShopTitle(args[2], String.join(" ", Arrays.copyOfRange(args, 3, args.length))) ? "shop-saved" : "shop-save-failed", Map.of("shop", args[2]));
            return;
        }
        if (action.equals("inventory") && args.length == 4) {
            message(sender, cosmoShopManager.setShopInventory(args[2], args[3]) ? "shop-saved" : "shop-save-failed", Map.of("shop", args[2]));
            return;
        }
        if (action.equals("item") && args.length == 6 && sender instanceof Player) {
            int slot = parseSlot(sender, args[3]);
            long buy = parseShopPrice(sender, args[4]);
            long sell = parseShopPrice(sender, args[5]);
            if (slot >= 0 && buy >= -1L && sell >= -1L && cosmoShopManager.setShopItem(args[2], slot, ((Player) sender).getInventory().getItemInMainHand(), buy, sell)) {
                message(sender, "shop-item-saved", Map.of("shop", args[2], "slot", String.valueOf(slot)));
                return;
            }
            message(sender, "shop-save-failed", Map.of("shop", args[2]));
            return;
        }
        if (action.equals("removeitem") && args.length == 4) {
            int slot = parseSlot(sender, args[3]);
            message(sender, slot >= 0 && cosmoShopManager.removeShopItem(args[2], slot) ? "shop-item-removed" : "shop-save-failed", Map.of("shop", args[2], "slot", args[3]));
            return;
        }
        usage(sender, "/cosmo shop <create <id> <cosmo>|delete <id>|title <id> <texto>|inventory <id> <inventario>|item <id> <slot> <compra|-1> <venta|-1>|removeitem <id> <slot>|reload>");
    }

    private void inventory(CommandSender sender, String[] args) {
        if (!admin(sender)) {
            return;
        }
        if (args.length < 2) {
            usage(sender, "/cosmo inventory <list|create|delete|title|size|item|removeitem|reload>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list") && args.length == 2) {
            message(sender, "inventories-list", Map.of("inventories", String.join("&7, &f", cosmoShopManager.getInventoryIds())));
            return;
        }
        if (action.equals("reload") && args.length == 2) {
            cosmoShopManager.reload();
            message(sender, "shops-reloaded", Map.of());
            return;
        }
        if (action.equals("create") && args.length == 3) {
            message(sender, cosmoShopManager.createInventory(args[2]) ? "inventory-created" : "inventory-save-failed", Map.of("inventory", args[2]));
            return;
        }
        if (action.equals("delete") && args.length == 3) {
            message(sender, cosmoShopManager.deleteInventory(args[2]) ? "inventory-deleted" : "inventory-save-failed", Map.of("inventory", args[2]));
            return;
        }
        if (action.equals("title") && args.length >= 4) {
            message(sender, cosmoShopManager.setInventoryTitle(args[2], String.join(" ", Arrays.copyOfRange(args, 3, args.length))) ? "inventory-saved" : "inventory-save-failed", Map.of("inventory", args[2]));
            return;
        }
        if (action.equals("size") && args.length == 4) {
            int size = parseSlot(sender, args[3]);
            message(sender, size >= 0 && cosmoShopManager.setInventorySize(args[2], size) ? "inventory-saved" : "inventory-save-failed", Map.of("inventory", args[2]));
            return;
        }
        if (action.equals("item") && args.length == 4 && sender instanceof Player) {
            int slot = parseSlot(sender, args[3]);
            if (slot >= 0 && cosmoShopManager.setInventoryItem(args[2], slot, ((Player) sender).getInventory().getItemInMainHand())) {
                message(sender, "inventory-item-saved", Map.of("inventory", args[2], "slot", String.valueOf(slot)));
                return;
            }
            message(sender, "inventory-save-failed", Map.of("inventory", args[2]));
            return;
        }
        if (action.equals("removeitem") && args.length == 4) {
            int slot = parseSlot(sender, args[3]);
            message(sender, slot >= 0 && cosmoShopManager.removeInventoryItem(args[2], slot) ? "inventory-item-removed" : "inventory-save-failed", Map.of("inventory", args[2], "slot", args[3]));
            return;
        }
        usage(sender, "/cosmo inventory <list|create <id>|delete <id>|title <id> <texto>|size <id> <9-54>|item <id> <slot>|removeitem <id> <slot>|reload>");
    }

    private boolean isShopAdminAction(String action) {
        return Arrays.asList("create", "delete", "title", "inventory", "item", "removeitem", "reload").contains(action.toLowerCase(Locale.ROOT));
    }

    private int parseSlot(CommandSender sender, String input) {
        try {
            int value = Integer.parseInt(input);
            if (value >= 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        message(sender, "invalid-number", Map.of());
        return -1;
    }

    private long parseShopPrice(CommandSender sender, String input) {
        try {
            long value = Long.parseLong(input);
            if (value >= -1L) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        message(sender, "invalid-shop-price", Map.of());
        return -2L;
    }

    private void hologram(CommandSender sender, String[] args) {
        if (!plugin.areHologramsEnabled()) {
            message(sender, "holograms-disabled", Map.of());
            return;
        }
        if (!admin(sender) || args.length < 2) {
            usage(sender, "/cosmo hologram <generate|move|title|delete|list> ...");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list") && args.length == 2) {
            List<String> status = hologramManager.getStatus();
            message(sender, status.isEmpty() ? "no-holograms" : "hologram-status-header", Map.of());
            for (String line : status) {
                sender.sendMessage(ColorUtil.color(line));
            }
            return;
        }
        if (action.equals("generate") || action.equals("create")) {
            if (!(sender instanceof Player) || args.length != 4) {
                usage(sender, "/cosmo hologram generate <id> <cosmo>");
                return;
            }
            if (!validId(args[2])) {
                message(sender, "invalid-name", Map.of());
                return;
            }
            if (!hologramManager.isAvailable()) {
                message(sender, "decent-holograms-required", Map.of());
                return;
            }
            message(sender, hologramManager.generate(args[2], args[3], ((Player) sender).getLocation()) ? "hologram-generated" : "hologram-generate-failed", Map.of());
            return;
        }
        if (action.equals("move")) {
            if (!(sender instanceof Player) || args.length != 3) {
                usage(sender, "/cosmo hologram move <id>");
                return;
            }
            message(sender, hologramManager.move(args[2], ((Player) sender).getLocation()) ? "hologram-moved" : "no-hologram", Map.of());
            return;
        }
        if (action.equals("title")) {
            if (args.length < 4) {
                usage(sender, "/cosmo hologram title <id> <título>");
                return;
            }
            String title = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            message(sender, hologramManager.setTitle(args[2], title) ? "hologram-title-updated" : "invalid-hologram-title", Map.of());
            return;
        }
        if (action.equals("delete") && args.length == 3) {
            message(sender, hologramManager.delete(args[2]) ? "hologram-deleted" : "no-hologram", Map.of());
            return;
        }
        usage(sender, "/cosmo hologram <generate|move|title|delete|list> ...");
    }

    private void reload(CommandSender sender) {
        if (!admin(sender)) {
            return;
        }
        plugin.reloadCosmos();
        message(sender, "reloaded", Map.of());
    }

    private void view(CommandSender sender) {
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!(sender instanceof Player)) {
            message(sender, "player-only", Map.of());
            return;
        }
        menuManager.openCosmosView((Player) sender, 0);
    }

    private void list(CommandSender sender) {
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        List<CosmoDefinition> cosmos = cosmosService.getCosmos();
        if (cosmos.isEmpty()) {
            message(sender, "no-cosmos", Map.of());
            return;
        }
        message(sender, "cosmo-list-header", Map.of());
        for (CosmoDefinition cosmo : cosmos) {
            message(sender, "cosmo-list-entry", Map.of(
                "cosmo", cosmo.getId(),
                "display-name", ColorUtil.color(cosmo.getDisplayName()),
                "trigger", cosmo.getTrigger().name(),
                "reward", NumberFormatUtil.format(cosmo.getReward()),
                "status", cosmo.isEnabled() ? "&aActivado" : "&cDesactivado"
            ));
        }
    }

    private void status(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 3) {
            usage(sender, "/cosmo status <cosmo> <enable|disable>");
            return;
        }
        boolean enabled;
        if (args[2].equalsIgnoreCase("enable") || args[2].equalsIgnoreCase("enabled")) {
            enabled = true;
        } else if (args[2].equalsIgnoreCase("disable") || args[2].equalsIgnoreCase("disabled")) {
            enabled = false;
        } else {
            usage(sender, "/cosmo status <cosmo> <enable|disable>");
            return;
        }
        if (!cosmosService.setEnabled(args[1], enabled)) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        message(sender, enabled ? "cosmo-enabled" : "cosmo-disabled", Map.of("cosmo", args[1]));
    }

    private void tops(CommandSender sender) {
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!(sender instanceof Player)) {
            message(sender, "player-only", Map.of());
            return;
        }
        menuManager.openTopSelection((Player) sender);
    }

    private void balance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!(sender instanceof Player)) {
            message(sender, "player-only", Map.of());
            return;
        }
        if (args.length != 2) {
            usage(sender, "/cosmo balance <cosmo>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        long playerBalance = cosmosService.getBalance(((Player) sender).getUniqueId(), cosmo.getId());
        message(sender, "balance", Map.of(
            "cosmo", ColorUtil.color(cosmo.getDisplayName()),
            "balance", NumberFormatUtil.format(playerBalance)
        ));
    }

    private void baltop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cosmos.view")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (args.length != 2) {
            usage(sender, "/cosmo baltop <cosmo>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        List<Map.Entry<java.util.UUID, Long>> top = cosmosService.getTop(cosmo.getId(), 15);
        if (top.isEmpty()) {
            message(sender, "no-ranked-players", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
            return;
        }
        message(sender, "baltop-header", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
        for (int index = 0; index < top.size(); index++) {
            Map.Entry<java.util.UUID, Long> entry = top.get(index);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = player.getName() == null ? entry.getKey().toString() : player.getName();
            message(sender, "baltop-entry", Map.of(
                "position", String.valueOf(index + 1),
                "player", playerName,
                "balance", NumberFormatUtil.format(entry.getValue())
            ));
        }
    }

    private void sendCosmo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cosmos.send")) {
            message(sender, "no-permission", Map.of());
            return;
        }
        if (!(sender instanceof Player)) {
            message(sender, "player-only", Map.of());
            return;
        }
        if (args.length != 4) {
            usage(sender, "/cosmo send <cosmo> <jugador> <cantidad>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        long value = amount(sender, args[3]);
        Player target = Bukkit.getPlayerExact(args[2]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        if (target == null || value <= 0L) {
            message(sender, "invalid-target-or-amount", Map.of());
            return;
        }
        if (target.getUniqueId().equals(((Player) sender).getUniqueId())) {
            message(sender, "cannot-send-to-self", Map.of());
            return;
        }
        if (!cosmosService.transfer(((Player) sender).getUniqueId(), target.getUniqueId(), cosmo.getId(), value)) {
            message(sender, "insufficient-funds", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
            return;
        }
        message(sender, "sent", Map.of("amount", NumberFormatUtil.format(value), "cosmo", ColorUtil.color(cosmo.getDisplayName()), "player", target.getName()));
        message(target, "received", Map.of("amount", NumberFormatUtil.format(value), "cosmo", ColorUtil.color(cosmo.getDisplayName())));
    }

    private void changeBalance(CommandSender sender, String[] args, boolean set) {
        if (!admin(sender) || args.length != 4) {
            usage(sender, set ? "/cosmo set <cosmo> <jugador> <cantidad>" : "/cosmo give <cosmo> <jugador> <cantidad>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        long value = signedAmount(sender, args[3]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (set) {
            cosmosService.setBalance(target.getUniqueId(), cosmo.getId(), value);
        } else {
            cosmosService.adjustBalance(target.getUniqueId(), cosmo.getId(), value);
        }
        message(sender, "balance-adjusted", Map.of(
            "player", args[2],
            "amount", NumberFormatUtil.format(value),
            "cosmo", ColorUtil.color(cosmo.getDisplayName())
        ));
        if (target.isOnline() && target.getPlayer() != null) {
            message(target.getPlayer(), "balance-adjusted-target", Map.of("amount", NumberFormatUtil.format(value), "cosmo", ColorUtil.color(cosmo.getDisplayName())));
        }
    }

    private void giveAll(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 3) {
            usage(sender, "/cosmo giveall <cosmo> <cantidad>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        long value = amount(sender, args[2]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        if (value <= 0L) {
            return;
        }
        String displayName = ColorUtil.color(cosmo.getDisplayName());
        int recipients = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            cosmosService.adjustBalance(player.getUniqueId(), cosmo.getId(), value);
            recipients++;
        }
        Map<String, String> replacements = Map.of(
            "amount", NumberFormatUtil.format(value),
            "cosmo", displayName,
            "players", String.valueOf(recipients)
        );
        message(sender, "giveall-success", replacements);
        for (Player player : Bukkit.getOnlinePlayers()) {
            message(player, "giveall-announcement", replacements);
        }
        playGiveAllMelody();
    }

    private void takeBalance(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 4) {
            usage(sender, "/cosmo take <cosmo> <jugador> <cantidad>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        long value = amount(sender, args[3]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        if (value <= 0L) {
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        long balance = cosmosService.getBalance(target.getUniqueId(), cosmo.getId());
        long removed = Math.min(Math.max(0L, balance), value);
        cosmosService.setBalance(target.getUniqueId(), cosmo.getId(), Math.max(0L, balance - removed));
        Map<String, String> replacements = Map.of(
            "player", args[2],
            "amount", NumberFormatUtil.format(removed),
            "cosmo", ColorUtil.color(cosmo.getDisplayName())
        );
        message(sender, "balance-taken", replacements);
        if (target.isOnline() && target.getPlayer() != null) {
            message(target.getPlayer(), "balance-taken-target", replacements);
        }
    }

    private void takeAll(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 3) {
            usage(sender, "/cosmo takeall <cosmo> <cantidad>");
            return;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(args[1]);
        long value = amount(sender, args[2]);
        if (cosmo == null) {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
            return;
        }
        if (value <= 0L) {
            return;
        }
        long removed = 0L;
        int recipients = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            long balance = cosmosService.getBalance(player.getUniqueId(), cosmo.getId());
            long playerRemoved = Math.min(Math.max(0L, balance), value);
            removed += playerRemoved;
            cosmosService.setBalance(player.getUniqueId(), cosmo.getId(), balance - playerRemoved);
            recipients++;
        }
        Map<String, String> replacements = Map.of(
            "amount", NumberFormatUtil.format(removed),
            "requested", NumberFormatUtil.format(value),
            "cosmo", ColorUtil.color(cosmo.getDisplayName()),
            "players", String.valueOf(recipients)
        );
        message(sender, "takeall-success", replacements);
        for (Player player : Bukkit.getOnlinePlayers()) {
            message(player, "takeall-announcement", replacements);
        }
    }

    private void playGiveAllMelody() {
        String[] melody = {"BLOCK_NOTE_BLOCK_HARP", "BLOCK_NOTE_BLOCK_HARP", "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_BLOCK_HARP", "BLOCK_NOTE_BLOCK_PLING"};
        for (int index = 0; index < melody.length; index++) {
            String soundName = melody[index];
            Bukkit.getScheduler().runTaskLater(plugin, () -> XSound.matchXSound(soundName).ifPresent(sound -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sound.play(player);
                }
            }), index * 4L);
        }
    }

    private void help(CommandSender sender) {
        sendLines(sender, "help.player");
        if (sender.hasPermission("cosmos.admin")) {
            sendLines(sender, "help.admin");
        }
    }

    private boolean admin(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender || sender.hasPermission("cosmos.admin")) {
            return true;
        }
        message(sender, "no-permission", Map.of());
        return false;
    }

    private long amount(CommandSender sender, String input) {
        try {
            long value = Long.parseLong(input);
            if (value > 0L) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        message(sender, "invalid-number", Map.of());
        return -1L;
    }

    private long signedAmount(CommandSender sender, String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException ignored) {
            message(sender, "invalid-integer", Map.of());
            return 0L;
        }
    }

    private boolean validId(String input) {
        return input.matches("[A-Za-z0-9_-]{3,24}");
    }

    private String normalizeMenuType(String input) {
        if (input.equalsIgnoreCase("cofre") || input.equalsIgnoreCase("chest")) {
            return "CHEST";
        }
        if (input.equalsIgnoreCase("dispensador") || input.equalsIgnoreCase("dispenser")) {
            return "DISPENSER";
        }
        if (input.equalsIgnoreCase("horno") || input.equalsIgnoreCase("furnace")) {
            return "FURNACE";
        }
        if (input.equalsIgnoreCase("soporte") || input.equalsIgnoreCase("pociones") || input.equalsIgnoreCase("brewing") || input.equalsIgnoreCase("brewing_stand")) {
            return "BREWING";
        }
        if (input.equalsIgnoreCase("tolva") || input.equalsIgnoreCase("hopper")) {
            return "HOPPER";
        }
        return null;
    }

    private boolean validMenuSize(String type, int size) {
        if (type.equals("CHEST")) {
            return size >= 9 && size <= 54 && size % 9 == 0;
        }
        return (type.equals("DISPENSER") && size == 9)
            || (type.equals("FURNACE") && size == 3)
            || ((type.equals("BREWING") || type.equals("HOPPER")) && size == 5);
    }

    private void usage(CommandSender sender, String usage) {
        message(sender, "usage", Map.of("usage", usage));
    }

    private void message(CommandSender sender, String key, Map<String, String> replacements) {
        plugin.getMessageManager().send(sender, key, replacements);
    }

    private void sendLines(CommandSender sender, String key) {
        for (String line : plugin.getMessageManager().formatList(key, Map.of())) {
            sender.sendMessage(ColorUtil.color(line));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return complete(args[0], Arrays.asList("create", "edit", "pars", "displayname", "delete", "menu", "menus", "shop", "inventory", "hologram", "reload", "view", "list", "status", "tops", "balance", "baltop", "send", "give", "take", "giveall", "takeall", "set", "help"));
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        if (root.equals("pars") && args.length == 2) {
            return complete(args[1], Collections.singletonList("set"));
        }
        if (root.equals("pars") && args.length == 3 && args[1].equalsIgnoreCase("set")) {
            return complete(args[2], cosmoIds());
        }
        if ((root.equals("edit") || root.equals("displayname") || root.equals("delete") || root.equals("status") || root.equals("balance") || root.equals("baltop") || root.equals("send") || root.equals("give") || root.equals("take") || root.equals("giveall") || root.equals("takeall") || root.equals("set")) && args.length == 2) {
            return complete(args[1], cosmoIds());
        }
        if (root.equals("status") && args.length == 3) {
            return complete(args[2], Arrays.asList("enable", "disable"));
        }
        if (root.equals("edit") && args.length == 3) {
            return complete(args[2], Arrays.asList("displayname", "type", "reward", "interval"));
        }
        if (root.equals("edit") && args.length == 4 && args[2].equalsIgnoreCase("type")) {
            return complete(args[3], Arrays.stream(CosmosTrigger.values()).map(Enum::name).collect(Collectors.toList()));
        }
        if (root.equals("edit") && args.length == 5 && args[2].equalsIgnoreCase("interval")) {
            return complete(args[4], Arrays.asList("minuto", "dia", "semana", "mes", "año"));
        }
        if ((root.equals("send") || root.equals("give") || root.equals("take") || root.equals("set")) && args.length == 3) {
            return complete(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        }
        if (root.equals("create") && args.length == 3) {
            return complete(args[2], Arrays.stream(CosmosTrigger.values()).map(Enum::name).collect(Collectors.toList()));
        }
        if (root.equals("create") && args.length == 6 && args[2].equalsIgnoreCase("TIME")) {
            return complete(args[5], Arrays.asList("minuto", "dia", "semana", "mes", "año"));
        }
        if (root.equals("menu")) {
            if (args.length == 2) {
                return complete(args[1], Arrays.asList("create", "open", "edit", "item", "displayname", "enabled", "disabled", "status", "delete"));
            }
            if ((args[1].equalsIgnoreCase("open") || args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("item") || args[1].equalsIgnoreCase("displayname") || args[1].equalsIgnoreCase("enabled") || args[1].equalsIgnoreCase("disabled") || args[1].equalsIgnoreCase("delete")) && args.length == 3) {
                return complete(args[2], menuManager.getMenuIds());
            }
            if (args[1].equalsIgnoreCase("item") && args.length == 5) {
                return complete(args[4], cosmoIds());
            }
            if (args[1].equalsIgnoreCase("create") && args.length == 4) {
                return complete(args[3], Arrays.asList("cofre", "dispensador", "horno", "soporte", "tolva"));
            }
            if (args[1].equalsIgnoreCase("create") && args.length == 5) {
                String type = normalizeMenuType(args[3]);
                if ("CHEST".equals(type)) {
                    return complete(args[4], Arrays.asList("9", "18", "27", "36", "45", "54"));
                }
                if ("DISPENSER".equals(type)) {
                    return complete(args[4], Collections.singletonList("9"));
                }
                if ("FURNACE".equals(type)) {
                    return complete(args[4], Collections.singletonList("3"));
                }
                if ("BREWING".equals(type) || "HOPPER".equals(type)) {
                    return complete(args[4], Collections.singletonList("5"));
                }
            }
        }
        if ((root.equals("shop") || root.equals("shops")) && args.length == 2) {
            return complete(args[1], Arrays.asList("menu", "list", "open", "create", "delete", "title", "inventory", "item", "removeitem", "reload"));
        }
        if ((root.equals("shop") || root.equals("shops")) && args.length == 3 && args[1].equalsIgnoreCase("open")) {
            return complete(args[2], cosmoShopManager.getShopIds());
        }
        if ((root.equals("shop") || root.equals("shops")) && args.length == 3 && Arrays.asList("delete", "title", "inventory", "item", "removeitem").contains(args[1].toLowerCase(Locale.ROOT))) {
            return complete(args[2], cosmoShopManager.getShopIds());
        }
        if ((root.equals("shop") || root.equals("shops")) && args.length == 4 && args[1].equalsIgnoreCase("inventory")) {
            return complete(args[3], cosmoShopManager.getInventoryIds());
        }
        if ((root.equals("shop") || root.equals("shops")) && args.length == 4 && args[1].equalsIgnoreCase("create")) {
            return complete(args[3], cosmoIds());
        }
        if ((root.equals("inventory") || root.equals("inventories")) && args.length == 2) {
            return complete(args[1], Arrays.asList("list", "create", "delete", "title", "size", "item", "removeitem", "reload"));
        }
        if ((root.equals("inventory") || root.equals("inventories")) && args.length == 3 && Arrays.asList("delete", "title", "size", "item", "removeitem").contains(args[1].toLowerCase(Locale.ROOT))) {
            return complete(args[2], cosmoShopManager.getInventoryIds());
        }
        if (root.equals("hologram") || root.equals("holograms")) {
            if (args.length == 2) {
                return complete(args[1], Arrays.asList("generate", "move", "title", "delete", "list"));
            }
            if ((args[1].equalsIgnoreCase("move") || args[1].equalsIgnoreCase("title") || args[1].equalsIgnoreCase("delete")) && args.length == 3) {
                return complete(args[2], hologramManager.getHologramIds());
            }
            if ((args[1].equalsIgnoreCase("generate") || args[1].equalsIgnoreCase("create")) && args.length == 4) {
                return complete(args[3], cosmoIds());
            }
        }
        return Collections.emptyList();
    }

    private List<String> cosmoIds() {
        return cosmosService.getCosmos().stream().map(CosmoDefinition::getId).collect(Collectors.toList());
    }

    private List<String> complete(String input, List<String> options) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowered)).collect(Collectors.toList());
    }
}