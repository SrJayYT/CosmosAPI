package com.devpapo.cosmosapi.command;

import com.devpapo.cosmosapi.shop.CosmoShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public final class PublicShopCommand extends Command {
    private final String shopId;
    private final CosmoShopManager shopManager;
    private final Map<String, String> subcommands;

    public PublicShopCommand(String command, String shopId, CosmoShopManager shopManager) {
        super(command);
        this.shopId = shopId;
        this.shopManager = shopManager;
        this.subcommands = null;
    }

    public PublicShopCommand(String command, Map<String, String> subcommands, CosmoShopManager shopManager) {
        super(command);
        this.shopId = null;
        this.shopManager = shopManager;
        this.subcommands = subcommands;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (subcommands == null) {
            shopManager.openShop((Player) sender, shopId);
            return true;
        }
        if (args.length == 1) {
            String targetShopId = subcommands.get(args[0].toLowerCase(Locale.ROOT));
            if (targetShopId != null) {
                shopManager.openShop((Player) sender, targetShopId);
            }
        }
        return true;
    }
}