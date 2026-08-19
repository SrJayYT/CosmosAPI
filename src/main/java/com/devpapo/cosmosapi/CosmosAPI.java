package com.devpapo.cosmosapi;

import com.devpapo.cosmosapi.command.CosmoCommand;
import com.devpapo.cosmosapi.command.PublicMenuCommand;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.economy.EconomyShopGUIListener;
import com.devpapo.cosmosapi.hologram.HologramManager;
import com.devpapo.cosmosapi.listener.CosmoRewardListener;
import com.devpapo.cosmosapi.menu.MenuListener;
import com.devpapo.cosmosapi.menu.MenuManager;
import com.devpapo.cosmosapi.placeholder.CosmosPlaceholderExpansion;
import com.devpapo.cosmosapi.storage.CosmosStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class CosmosAPI extends JavaPlugin {
    private static CosmosAPI instance;

    private CosmosStorage storage;
    private CosmosService cosmosService;
    private MenuManager menuManager;
    private HologramManager hologramManager;
    private final Map<String, Command> registeredMenuCommands = new HashMap<>();

    public static CosmosAPI getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("cosmos.yml", false);
        saveResource("menus.yml", false);
        saveResource("players.yml", false);

        storage = new CosmosStorage(this);
        cosmosService = new CosmosService(storage);
        menuManager = new MenuManager(this, storage, cosmosService);
        hologramManager = new HologramManager(this, storage, cosmosService);

        CosmoCommand command = new CosmoCommand(this, cosmosService, menuManager, hologramManager);
        PluginCommand cosmoCommand = getCommand("cosmo");
        if (cosmoCommand == null) {
            getLogger().severe("No se pudo registrar el comando /cosmo.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cosmoCommand.setExecutor(command);
        cosmoCommand.setTabCompleter(command);

        registerMenuCommands();

        Bukkit.getPluginManager().registerEvents(new CosmoRewardListener(this, cosmosService), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(menuManager), this);
        menuManager.startTimeRewards();
        hologramManager.start();

        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null) {
            Bukkit.getPluginManager().registerEvents(new EconomyShopGUIListener(cosmosService), this);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CosmosPlaceholderExpansion(this, cosmosService).register();
        }
    }

    @Override
    public void onDisable() {
        unregisterMenuCommands();
        if (menuManager != null) {
            menuManager.stopTimeRewards();
        }
        if (hologramManager != null) {
            hologramManager.stop();
        }
    }

    public CosmosService getCosmosService() {
        return cosmosService;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public void reloadCosmos() {
        reloadConfig();
        storage.reload();
        cosmosService.reload();
        menuManager.restartTimeRewards();
        hologramManager.reload();
        registerMenuCommands();
    }

    private void registerMenuCommands() {
        unregisterMenuCommands();
        CommandMap commandMap = Bukkit.getCommandMap();
        for (Map.Entry<String, String> entry : menuManager.getPublicMenuCommands().entrySet()) {
            String commandName = entry.getKey();
            if (commandMap.getCommand(commandName) != null) {
                getLogger().warning("No se registró /" + commandName + " para el menú " + entry.getValue() + " porque ese comando ya existe.");
                continue;
            }
            Command command = new PublicMenuCommand(commandName, entry.getValue(), menuManager);
            if (commandMap.register(getName().toLowerCase(), command)) {
                registeredMenuCommands.put(commandName, command);
            } else {
                getLogger().warning("No se pudo registrar /" + commandName + " para el menú " + entry.getValue() + ".");
            }
        }
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    private void unregisterMenuCommands() {
        if (registeredMenuCommands.isEmpty()) {
            return;
        }
        CommandMap commandMap = Bukkit.getCommandMap();
        for (Command command : registeredMenuCommands.values()) {
            command.unregister(commandMap);
        }
        registeredMenuCommands.clear();
    }
}