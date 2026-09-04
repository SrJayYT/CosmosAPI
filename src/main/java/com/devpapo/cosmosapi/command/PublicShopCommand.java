package com.devpapo.cosmosapi.command;

import com.devpapo.cosmosapi.shop.CosmoShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PublicShopCommand extends Command {
    private final String shopId;
    private final CosmoShopManager shopManager;

    public PublicShopCommand(String command, String shopId, CosmoShopManager shopManager) {
        super(command);
        this.shopId = shopId;
        this.shopManager = shopManager;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            shopManager.openShop((Player) sender, shopId);
        }
        return true;
    }
}