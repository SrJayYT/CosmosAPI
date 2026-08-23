package com.devpapo.cosmosapi;

import com.devpapo.cosmosapi.command.CosmoCommand;
import com.devpapo.cosmosapi.command.ConditionsCommand;
import com.devpapo.cosmosapi.command.PublicMenuCommand;
import com.devpapo.cosmosapi.condition.ConditionsService;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.economy.EconomyShopGUIListener;
import com.devpapo.cosmosapi.hologram.HologramManager;
import com.devpapo.cosmosapi.listener.CosmoRewardListener;
import com.devpapo.cosmosapi.menu.MenuListener;
import com.devpapo.cosmosapi.menu.MenuManager;
import com.devpapo.cosmosapi.placeholder.CosmosPlaceholderExpansion;
import com.devpapo.cosmosapi.storage.CosmosStorage;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class CosmosAPI extends JavaPlugin {
    private static CosmosAPI instance;

    private CosmosStorage storage;
    private CosmosService cosmosService;
    private ConditionsService conditionsService;
    private MenuManager menuManager;
    private HologramManager hologramManager;
    private MessageManager messageManager;
    private BukkitTask memoryCleanupTask;
    private final Map<String, Command> registeredMenuCommands = new HashMap<>();

    public static CosmosAPI getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("cosmos.yml", false);
        saveResource("conditions.yml", false);
        saveResource("menus.yml", false);
        saveResource("players.yml", false);
        saveResource("messages.yml", false);

        messageManager = new MessageManager(this);
        storage = new CosmosStorage(this);
        cosmosService = new CosmosService(storage);
        conditionsService = new ConditionsService(storage, cosmosService);
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

        ConditionsCommand conditionsCommand = new ConditionsCommand(this, conditionsService, cosmosService);
        PluginCommand conditionsPluginCommand = getCommand("conditions");
        if (conditionsPluginCommand == null) {
            getLogger().severe("No se pudo registrar el comando /conditions.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        conditionsPluginCommand.setExecutor(conditionsCommand);
        conditionsPluginCommand.setTabCompleter(conditionsCommand);

        registerMenuCommands();

        Bukkit.getPluginManager().registerEvents(new CosmoRewardListener(this, cosmosService, conditionsService), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(menuManager), this);
        menuManager.startTimeRewards();
        runOptionalIntegration("DecentHolograms", hologramManager::start);
        startMemoryCleanupTask();

        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null) {
            runOptionalIntegration("EconomyShopGUI", () -> Bukkit.getPluginManager().registerEvents(new EconomyShopGUIListener(cosmosService), this));
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            runOptionalIntegration("PlaceholderAPI", () -> {
                new CosmosPlaceholderExpansion(this, cosmosService, "cosmos").register();
                new CosmosPlaceholderExpansion(this, cosmosService, "cosmosapi").register();
            });
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
        stopMemoryCleanupTask();
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

    public boolean areMenusEnabled() {
        return getConfig().getBoolean("menus.enabled", false);
    }

    public boolean areHologramsEnabled() {
        return getConfig().getBoolean("holograms.enabled", true);
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public void reloadCosmos() {
        reloadConfig();
        messageManager.reload();
        storage.reload();
        cosmosService.reload();
        conditionsService.reload();
        menuManager.restartTimeRewards();
        hologramManager.reload();
        restartMemoryCleanupTask();
        registerMenuCommands();
    }

    private void startMemoryCleanupTask() {
        long intervalMinutes = getConfig().getLong("memory-cleanup.interval-minutes", 0L);
        if (intervalMinutes <= 0L) {
            return;
        }
        long intervalTicks;
        try {
            intervalTicks = Math.multiplyExact(Math.multiplyExact(intervalMinutes, 60L), 20L);
        } catch (ArithmeticException exception) {
            getLogger().warning("El intervalo de memory-cleanup es demasiado grande. La limpieza automática fue desactivada.");
            return;
        }
        memoryCleanupTask = Bukkit.getScheduler().runTaskTimer(this, this::cleanupMemory, intervalTicks, intervalTicks);
    }

    private void runOptionalIntegration(String integrationName, Runnable action) {
        try {
            action.run();
        } catch (LinkageError | RuntimeException exception) {
            getLogger().warning("La integración con " + integrationName + " fue desactivada por incompatibilidad: " + exception.getClass().getSimpleName());
            getLogger().warning("Actualiza " + integrationName + " a una versión compatible con tu servidor.");
        }
    }

    private void restartMemoryCleanupTask() {
        stopMemoryCleanupTask();
        startMemoryCleanupTask();
    }

    private void stopMemoryCleanupTask() {
        if (memoryCleanupTask != null) {
            memoryCleanupTask.cancel();
            memoryCleanupTask = null;
        }
    }

    private void cleanupMemory() {
        System.gc();
        String message = messageManager.format("memory-cleanup", Map.of());
        getLogger().info(ColorUtil.color(message));
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                messageManager.send(player, "memory-cleanup", Map.of());
            }
        }
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