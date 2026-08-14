package com.devpapo.cosmosapi.command;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmosTrigger;
import com.devpapo.cosmosapi.cosmo.TimeRewardUnit;
import com.devpapo.cosmosapi.hologram.HologramManager;
import com.devpapo.cosmosapi.menu.MenuManager;
import com.devpapo.cosmosapi.util.ColorUtil;
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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CosmoCommand implements CommandExecutor, TabCompleter {
    private final CosmosAPI plugin;
    private final CosmosService cosmosService;
    private final MenuManager menuManager;
    private final HologramManager hologramManager;

    public CosmoCommand(CosmosAPI plugin, CosmosService cosmosService, MenuManager menuManager, HologramManager hologramManager) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
        this.menuManager = menuManager;
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
            case "tops":
                tops(sender);
                return true;
            case "send":
                sendCosmo(sender, args);
                return true;
            case "give":
                changeBalance(sender, args, false);
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
            sender.sendMessage(ColorUtil.color("&cTipos: " + Arrays.toString(CosmosTrigger.values())));
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
            sender.sendMessage(ColorUtil.color("&cYa existe un cosmo con ese nombre."));
            return;
        }
        String time = trigger == CosmosTrigger.TIME ? " cada &f" + interval + " " + unit.name() : "";
        sender.sendMessage(ColorUtil.color("&aCosmo &f" + args[1] + " &acreado. Recompensa: &f" + reward + " &apor &f" + trigger.name() + time));
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
        sender.sendMessage(ColorUtil.color("&aNombre visual actualizado."));
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
            sender.sendMessage(ColorUtil.color("&aCosmo actualizado."));
        } else {
            sender.sendMessage(ColorUtil.color("&cNo se pudo actualizar: revisa el cosmo y los valores. El intervalo solo aplica a TIME."));
        }
    }

    private void delete(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length != 2) {
            usage(sender, "/cosmo delete <cosmo>");
            return;
        }
        if (cosmosService.delete(args[1])) {
            hologramManager.deleteForCosmo(args[1]);
            sender.sendMessage(ColorUtil.color("&aCosmo eliminado."));
        } else {
            message(sender, "unknown-cosmo", Map.of("cosmo", args[1]));
        }
    }

    private void menu(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/cosmo menu <create|open|edit|item|displayname|status|delete> ...");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("status")) {
            if (!admin(sender) || args.length != 2) {
                usage(sender, "/cosmo menu status");
                return;
            }
            List<String> status = menuManager.getMenuStatus();
            sender.sendMessage(ColorUtil.color(status.isEmpty() ? "&eNo hay menús creados." : "&d&lEstado de menús:"));
            for (String line : status) {
                sender.sendMessage(ColorUtil.color(line));
            }
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
                sender.sendMessage(ColorUtil.color("&cCofre: 9,18,27,36,45,54. Dispensador: 9. Horno: 3. Soporte y tolva: 5."));
                return;
            }
            if (!menuManager.createMenu(args[2], type, size)) {
                sender.sendMessage(ColorUtil.color("&cYa existe un menú con ese nombre."));
                return;
            }
            sender.sendMessage(ColorUtil.color("&aMenú creado. Sostén un ítem y usa &f/cosmo menu item " + args[2] + " <slot> <cosmo> <precio> &apara añadir productos."));
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
                sender.sendMessage(ColorUtil.color("&cEse menú no existe."));
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
                sender.sendMessage(ColorUtil.color("&cRevisa el menú, slot, cosmo, precio y el ítem de tu mano."));
                return;
            }
            sender.sendMessage(ColorUtil.color("&aProducto actualizado."));
            return;
        }
        if (action.equals("displayname")) {
            if (!admin(sender) || args.length < 4) {
                usage(sender, "/cosmo menu displayname <menú> <nombre>");
                return;
            }
            sender.sendMessage(ColorUtil.color(menuManager.setDisplayName(args[2], String.join(" ", Arrays.copyOfRange(args, 3, args.length)))
                ? "&aNombre visual del menú actualizado."
                : "&cEse menú no existe."));
            return;
        }
        if (action.equals("delete")) {
            if (!admin(sender) || args.length != 3) {
                usage(sender, "/cosmo menu delete <nombre>");
                return;
            }
            sender.sendMessage(ColorUtil.color(menuManager.deleteMenu(args[2]) ? "&aMenú eliminado." : "&cEse menú no existe."));
            return;
        }
        usage(sender, "/cosmo menu <create|open|edit|item|displayname|status|delete> ...");
    }

    private void menus(CommandSender sender, String[] args) {
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
        menuManager.openMenuDirectory((Player) sender, Math.max(0, page - 1));
    }

    private void hologram(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length < 2) {
            usage(sender, "/cosmo hologram <generate|move|title|delete|list> ...");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list") && args.length == 2) {
            List<String> status = hologramManager.getStatus();
            sender.sendMessage(ColorUtil.color(status.isEmpty() ? "&eNo hay hologramas creados." : "&d&lHologramas de top:"));
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
                sender.sendMessage(ColorUtil.color("&cNecesitas instalar DecentHolograms para generar hologramas."));
                return;
            }
            sender.sendMessage(ColorUtil.color(hologramManager.generate(args[2], args[3], ((Player) sender).getLocation())
                ? "&aHolograma top 15 generado en tu ubicación."
                : "&cRevisa que el id sea único y que el cosmo exista."));
            return;
        }
        if (action.equals("move")) {
            if (!(sender instanceof Player) || args.length != 3) {
                usage(sender, "/cosmo hologram move <id>");
                return;
            }
            sender.sendMessage(ColorUtil.color(hologramManager.move(args[2], ((Player) sender).getLocation())
                ? "&aHolograma movido a tu ubicación actual."
                : "&cEse holograma no existe."));
            return;
        }
        if (action.equals("title")) {
            if (args.length < 4) {
                usage(sender, "/cosmo hologram title <id> <título>");
                return;
            }
            String title = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            sender.sendMessage(ColorUtil.color(hologramManager.setTitle(args[2], title)
                ? "&aTítulo del holograma actualizado."
                : "&cEse holograma no existe o el título no es válido."));
            return;
        }
        if (action.equals("delete") && args.length == 3) {
            sender.sendMessage(ColorUtil.color(hologramManager.delete(args[2]) ? "&aHolograma eliminado." : "&cEse holograma no existe."));
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
            sender.sendMessage(ColorUtil.color("&cEl jugador debe estar conectado y la cantidad ser válida."));
            return;
        }
        if (!cosmosService.transfer(((Player) sender).getUniqueId(), target.getUniqueId(), cosmo.getId(), value)) {
            message(sender, "insufficient-funds", Map.of("cosmo", ColorUtil.color(cosmo.getDisplayName())));
            return;
        }
        message(sender, "sent", Map.of("amount", String.valueOf(value), "cosmo", ColorUtil.color(cosmo.getDisplayName()), "player", target.getName()));
        message(target, "received", Map.of("amount", String.valueOf(value), "cosmo", ColorUtil.color(cosmo.getDisplayName())));
    }

    private void changeBalance(CommandSender sender, String[] args, boolean set) {
        if (!admin(sender) || args.length != 4) {
            usage(sender, set ? "/cosmo set <cosmo> <jugador> <cantidad>" : "/cosmo give <cosmo> <jugador> <cantidad>");
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
        if (set) {
            cosmosService.setBalance(target.getUniqueId(), cosmo.getId(), value);
        } else {
            cosmosService.deposit(target.getUniqueId(), cosmo.getId(), value);
        }
        sender.sendMessage(ColorUtil.color("&aSaldo actualizado para &f" + args[2] + "&a."));
        if (target.isOnline() && target.getPlayer() != null) {
            message(target.getPlayer(), "received", Map.of("amount", String.valueOf(value), "cosmo", ColorUtil.color(cosmo.getDisplayName())));
        }
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&d&lCosmosAPI &8• &fGuía de Cosmos"));
        sender.sendMessage(ColorUtil.color("&d/cosmo view &7- Ver tus cosmos y saldos."));
        sender.sendMessage(ColorUtil.color("&d/cosmo menus &7- Ver los menús públicos disponibles."));
        sender.sendMessage(ColorUtil.color("&d/cosmo tops &7- Consultar los rankings de cada cosmo."));
        sender.sendMessage(ColorUtil.color("&d/cosmo send <cosmo> <jugador> <cantidad> &7- Enviar cosmos."));
        if (sender.hasPermission("cosmos.admin")) {
            sender.sendMessage(ColorUtil.color("&5Admin: &f/cosmo create <nombre> <tipo> <cantidad> [intervalo] [unidad]"));
            sender.sendMessage(ColorUtil.color("&5Admin: &f/cosmo edit <cosmo> <displayname|type|reward|interval> <valor>"));
            sender.sendMessage(ColorUtil.color("&5Admin: &f/cosmo displayname, delete, give, set, reload"));
            sender.sendMessage(ColorUtil.color("&5Menús: &f/cosmo menu create|open|edit|item|displayname|status|delete"));
            sender.sendMessage(ColorUtil.color("&5Hologramas: &f/cosmo hologram generate <id> <cosmo>, move, title, delete, list"));
            sender.sendMessage(ColorUtil.color("&7Sostén el producto en tu mano y usa &f/cosmo menu item <menú> <slot> <cosmo> <precio>&7."));
        }
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("cosmos.admin")) {
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
        sender.sendMessage(ColorUtil.color("&cUso: " + usage));
    }

    private void message(CommandSender sender, String key, Map<String, String> replacements) {
        String message = plugin.getConfig().getString("messages." + key, "");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        sender.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix", "") + message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return complete(args[0], Arrays.asList("create", "edit", "displayname", "delete", "menu", "menus", "hologram", "reload", "view", "tops", "send", "give", "set", "help"));
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        if ((root.equals("edit") || root.equals("displayname") || root.equals("delete") || root.equals("send") || root.equals("give") || root.equals("set")) && args.length == 2) {
            return complete(args[1], cosmoIds());
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
        if ((root.equals("send") || root.equals("give") || root.equals("set")) && args.length == 3) {
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
                return complete(args[1], Arrays.asList("create", "open", "edit", "item", "displayname", "status", "delete"));
            }
            if ((args[1].equalsIgnoreCase("open") || args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("item") || args[1].equalsIgnoreCase("displayname") || args[1].equalsIgnoreCase("delete")) && args.length == 3) {
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