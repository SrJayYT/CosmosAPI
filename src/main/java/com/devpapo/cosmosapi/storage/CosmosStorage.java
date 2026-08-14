package com.devpapo.cosmosapi.storage;

import com.devpapo.cosmosapi.CosmosAPI;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class CosmosStorage {
    private final CosmosAPI plugin;
    private final File cosmosFile;
    private final File menusFile;
    private final File playersFile;
    private FileConfiguration cosmos;
    private FileConfiguration menus;
    private FileConfiguration players;

    public CosmosStorage(CosmosAPI plugin) {
        this.plugin = plugin;
        this.cosmosFile = new File(plugin.getDataFolder(), "cosmos.yml");
        this.menusFile = new File(plugin.getDataFolder(), "menus.yml");
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public void reload() {
        cosmos = YamlConfiguration.loadConfiguration(cosmosFile);
        menus = YamlConfiguration.loadConfiguration(menusFile);
        players = YamlConfiguration.loadConfiguration(playersFile);
    }

    public FileConfiguration getCosmos() {
        return cosmos;
    }

    public FileConfiguration getMenus() {
        return menus;
    }

    public FileConfiguration getPlayers() {
        return players;
    }

    public Set<String> getCosmoIds() {
        if (cosmos.getConfigurationSection("cosmos") == null) {
            return Collections.emptySet();
        }
        return cosmos.getConfigurationSection("cosmos").getKeys(false);
    }

    public Set<String> getMenuIds() {
        if (menus.getConfigurationSection("menus") == null) {
            return Collections.emptySet();
        }
        return menus.getConfigurationSection("menus").getKeys(false);
    }

    public void saveCosmos() {
        save(cosmos, cosmosFile);
    }

    public void saveMenus() {
        save(menus, menusFile);
    }

    public void savePlayers() {
        save(players, playersFile);
    }

    private void save(FileConfiguration configuration, File file) {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + file.getName(), exception);
        }
    }
}