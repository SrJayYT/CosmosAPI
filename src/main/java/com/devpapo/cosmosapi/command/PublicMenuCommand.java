package com.devpapo.cosmosapi.command;

import com.devpapo.cosmosapi.menu.MenuManager;
import com.devpapo.cosmosapi.util.ColorUtil;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PublicMenuCommand extends Command {
    private final String menuId;
    private final MenuManager menuManager;

    public PublicMenuCommand(String command, String menuId, MenuManager menuManager) {
        super(command);
        this.menuId = menuId;
        this.menuManager = menuManager;
        setDescription("Abre el menú " + menuId + ".");
        setUsage("/" + command);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.color("&cEste comando solo puede usarse dentro del servidor."));
            return true;
        }
        menuManager.openShop((Player) sender, menuId);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return Collections.emptyList();
    }
}