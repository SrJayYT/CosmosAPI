package com.devpapo.cosmosapi.command;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.condition.ConditionDefinition;
import com.devpapo.cosmosapi.condition.ConditionsService;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmosTrigger;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class ConditionsCommand implements CommandExecutor, TabCompleter {
    private final CosmosAPI plugin;
    private final ConditionsService conditionsService;
    private final CosmosService cosmosService;

    public ConditionsCommand(CosmosAPI plugin, ConditionsService conditionsService, CosmosService cosmosService) {
        this.plugin = plugin;
        this.conditionsService = conditionsService;
        this.cosmosService = cosmosService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cosmos.conditions")) {
            message(sender, "no-permission", Map.of());
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                create(sender, args);
                return true;
            case "list":
                list(sender);
                return true;
            case "info":
                info(sender, args);
                return true;
            case "edit":
                edit(sender, args);
                return true;
            case "delete":
                delete(sender, args);
                return true;
            case "reload":
                plugin.reloadCosmos();
                message(sender, "reloaded", Map.of());
                return true;
            default:
                help(sender);
                return true;
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length != 5) {
            usage(sender, "/conditions create <id> <cosmo> <tipo> <cantidad>");
            return;
        }
        CosmosTrigger trigger = CosmosTrigger.fromInput(args[3]);
        long amount = amount(sender, args[4]);
        if (!validId(args[1]) || !conditionsService.isSupportedTrigger(trigger) || amount <= 0L || !conditionsService.create(args[1], args[2], trigger, amount)) {
            message(sender, "condition-create-failed", Map.of());
            return;
        }
        message(sender, "condition-created", Map.of("condition", args[1]));
    }

    private void list(CommandSender sender) {
        List<ConditionDefinition> conditions = conditionsService.getConditions();
        if (conditions.isEmpty()) {
            message(sender, "no-conditions", Map.of());
            return;
        }
        message(sender, "condition-list-header", Map.of());
        for (ConditionDefinition condition : conditions) {
            message(sender, "condition-list-entry", Map.of(
                "condition", condition.getId(),
                "cosmo", condition.getCosmoId(),
                "trigger", condition.getTrigger().name(),
                "amount", NumberFormatUtil.format(condition.getAmount())
            ));
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length != 2) {
            usage(sender, "/conditions info <id>");
            return;
        }
        ConditionDefinition condition = conditionsService.getCondition(args[1]);
        if (condition == null) {
            message(sender, "unknown-condition", Map.of("condition", args[1]));
            return;
        }
        message(sender, "condition-info", Map.of(
            "condition", condition.getId(),
            "cosmo", condition.getCosmoId(),
            "trigger", condition.getTrigger().name(),
            "amount", NumberFormatUtil.format(condition.getAmount())
        ));
    }

    private void edit(CommandSender sender, String[] args) {
        if (args.length != 4) {
            usage(sender, "/conditions edit <id> <cosmo|type|amount> <valor>");
            return;
        }
        boolean updated;
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "cosmo":
                updated = conditionsService.setCosmo(args[1], args[3]);
                break;
            case "type":
                updated = conditionsService.setTrigger(args[1], CosmosTrigger.fromInput(args[3]));
                break;
            case "amount":
                updated = conditionsService.setAmount(args[1], amount(sender, args[3]));
                break;
            default:
                usage(sender, "/conditions edit <id> <cosmo|type|amount> <valor>");
                return;
        }
        message(sender, updated ? "condition-updated" : "condition-update-failed", Map.of());
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            usage(sender, "/conditions delete <id>");
            return;
        }
        message(sender, conditionsService.delete(args[1]) ? "condition-deleted" : "unknown-condition", Map.of("condition", args[1]));
    }

    private void help(CommandSender sender) {
        message(sender, "conditions-help", Map.of());
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

    private void usage(CommandSender sender, String usage) {
        message(sender, "usage", Map.of("usage", usage));
    }

    private void message(CommandSender sender, String key, Map<String, String> replacements) {
        plugin.getMessageManager().send(sender, key, replacements);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return complete(args[0], Arrays.asList("create", "list", "info", "edit", "delete", "reload", "help"));
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if ((action.equals("info") || action.equals("edit") || action.equals("delete")) && args.length == 2) {
            return complete(args[1], conditionIds());
        }
        if (action.equals("create") && args.length == 3) {
            return complete(args[2], cosmoIds());
        }
        if (action.equals("create") && args.length == 4) {
            return complete(args[3], triggerNames());
        }
        if (action.equals("edit") && args.length == 3) {
            return complete(args[2], Arrays.asList("cosmo", "type", "amount"));
        }
        if (action.equals("edit") && args.length == 4 && args[2].equalsIgnoreCase("cosmo")) {
            return complete(args[3], cosmoIds());
        }
        if (action.equals("edit") && args.length == 4 && args[2].equalsIgnoreCase("type")) {
            return complete(args[3], triggerNames());
        }
        return Collections.emptyList();
    }

    private List<String> conditionIds() {
        return conditionsService.getConditions().stream().map(ConditionDefinition::getId).collect(Collectors.toList());
    }

    private List<String> cosmoIds() {
        return cosmosService.getCosmos().stream().map(CosmoDefinition::getId).collect(Collectors.toList());
    }

    private List<String> triggerNames() {
        return Arrays.stream(CosmosTrigger.values()).filter(conditionsService::isSupportedTrigger).map(Enum::name).collect(Collectors.toList());
    }

    private List<String> complete(String input, List<String> options) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowered)).collect(Collectors.toList());
    }
}