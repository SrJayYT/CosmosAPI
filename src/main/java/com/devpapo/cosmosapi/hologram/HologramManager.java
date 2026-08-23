package com.devpapo.cosmosapi.hologram;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.storage.CosmosStorage;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

public final class HologramManager {
    private static final String PATH = "holograms";
    private static final String PREFIX = "cosmosapi_top_";

    private final CosmosAPI plugin;
    private final CosmosStorage storage;
    private final CosmosService cosmosService;
    private BukkitTask refreshTask;
    private boolean unavailableLogged;

    public HologramManager(CosmosAPI plugin, CosmosStorage storage, CosmosService cosmosService) {
        this.plugin = plugin;
        this.storage = storage;
        this.cosmosService = cosmosService;
    }

    public boolean isAvailable() {
        try {
            return Bukkit.getPluginManager().getPlugin("DecentHolograms") != null
                && Class.forName("eu.decentsoftware.holograms.api.DHAPI") != null;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public void start() {
        if (!plugin.areHologramsEnabled()) {
            removeAllHolograms();
            return;
        }
        refreshAll();
        if (isAvailable()) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 1200L, 1200L);
        } else if (!unavailableLogged) {
            unavailableLogged = true;
            plugin.getLogger().warning("DecentHolograms no está instalado; los hologramas de CosmosAPI permanecerán desactivados.");
        }
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void reload() {
        stop();
        start();
    }

    public boolean generate(String id, String cosmoId, Location location) {
        String key = normalize(id);
        if (!plugin.areHologramsEnabled() || !isAvailable() || cosmosService.getCosmo(cosmoId) == null || getSection(key) != null || location.getWorld() == null) {
            return false;
        }
        saveLocation(key, cosmosService.getCosmo(cosmoId).getId(), location);
        storage.saveCosmos();
        refresh(key);
        return true;
    }

    public boolean move(String id, Location location) {
        String key = normalize(id);
        ConfigurationSection section = getSection(key);
        if (section == null || location.getWorld() == null) {
            return false;
        }
        saveLocation(key, section.getString("cosmo", ""), location);
        storage.saveCosmos();
        refresh(key);
        return true;
    }

    public boolean setTitle(String id, String title) {
        String key = normalize(id);
        if (getSection(key) == null || title.trim().isEmpty()) {
            return false;
        }
        storage.getCosmos().set(PATH + "." + key + ".title", title);
        storage.saveCosmos();
        refresh(key);
        return true;
    }

    public boolean delete(String id) {
        String key = normalize(id);
        if (getSection(key) == null) {
            return false;
        }
        removeHologram(key);
        storage.getCosmos().set(PATH + "." + key, null);
        storage.saveCosmos();
        return true;
    }

    public void deleteForCosmo(String cosmoId) {
        for (String id : new ArrayList<>(getHologramIds())) {
            ConfigurationSection section = getSection(id);
            if (section != null && section.getString("cosmo", "").equalsIgnoreCase(cosmoId)) {
                delete(id);
            }
        }
    }

    public List<String> getHologramIds() {
        ConfigurationSection section = storage.getCosmos().getConfigurationSection(PATH);
        if (section == null) {
            return new ArrayList<>();
        }
        List<String> ids = new ArrayList<>(section.getKeys(false));
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    public List<String> getStatus() {
        List<String> status = new ArrayList<>();
        for (String id : getHologramIds()) {
            ConfigurationSection section = getSection(id);
            if (section == null) {
                continue;
            }
            String cosmo = section.getString("cosmo", "?");
            String world = section.getString("world", "?");
            boolean valid = cosmosService.getCosmo(cosmo) != null && Bukkit.getWorld(world) != null;
            status.add(plugin.getMessageManager().format(valid ? "hologram-status.active" : "hologram-status.invalid", Map.of("id", id, "cosmo", cosmo, "world", world)));
        }
        return status;
    }

    public void refreshAll() {
        if (!plugin.areHologramsEnabled() || !isAvailable()) {
            return;
        }
        for (String id : getHologramIds()) {
            refresh(id);
        }
    }

    private void refresh(String id) {
        if (!plugin.areHologramsEnabled() || !isAvailable()) {
            return;
        }
        ConfigurationSection section = getSection(id);
        if (section == null) {
            return;
        }
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) {
            return;
        }
        Location location = new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
        String cosmoId = section.getString("cosmo", "");
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        List<String> lines = new ArrayList<>();
        if (cosmo == null) {
            lines.add(ColorUtil.color(plugin.getMessageManager().format("hologram.cosmo-unavailable", Map.of())));
        } else {
            lines.add(ColorUtil.color(section.getString("title", plugin.getMessageManager().format("hologram.default-title", Map.of("cosmo", cosmo.getDisplayName())))));
            List<Map.Entry<UUID, Long>> top = cosmosService.getTop(cosmo.getId(), 15);
            if (top.isEmpty()) {
                lines.add(ColorUtil.color(plugin.getMessageManager().format("hologram.no-ranked-players", Map.of())));
            } else {
                for (int index = 0; index < top.size(); index++) {
                    Map.Entry<UUID, Long> entry = top.get(index);
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                    String name = player.getName() == null ? entry.getKey().toString().substring(0, 8) : player.getName();
                    lines.add(ColorUtil.color(plugin.getMessageManager().format("hologram.entry", Map.of("position", String.valueOf(index + 1), "player", name, "balance", NumberFormatUtil.format(entry.getValue())))));
                }
            }
        }
        try {
            Class<?> apiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Class<?> hologramClass = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");
            String hologramName = hologramName(id);
            Object hologram = apiClass.getMethod("getHologram", String.class).invoke(null, hologramName);
            if (hologram == null) {
                hologram = apiClass.getMethod("createHologram", String.class, Location.class, boolean.class, List.class)
                    .invoke(null, hologramName, location, true, lines);
            } else {
                apiClass.getMethod("moveHologram", String.class, Location.class).invoke(null, hologramName, location);
                apiClass.getMethod("setHologramLines", hologramClass, List.class).invoke(null, hologram, lines);
            }
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("No se pudo actualizar el holograma de cosmos '" + id + "': " + exception.getMessage());
        }
    }

    private void removeHologram(String id) {
        if (!isAvailable()) {
            return;
        }
        try {
            Class.forName("eu.decentsoftware.holograms.api.DHAPI").getMethod("removeHologram", String.class).invoke(null, hologramName(id));
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("No se pudo eliminar el holograma de cosmos '" + id + "': " + exception.getMessage());
        }
    }

    private void removeAllHolograms() {
        for (String id : getHologramIds()) {
            removeHologram(id);
        }
    }

    private void saveLocation(String id, String cosmoId, Location location) {
        String path = PATH + "." + id;
        storage.getCosmos().set(path + ".cosmo", cosmoId);
        storage.getCosmos().set(path + ".world", location.getWorld().getName());
        storage.getCosmos().set(path + ".x", location.getX());
        storage.getCosmos().set(path + ".y", location.getY());
        storage.getCosmos().set(path + ".z", location.getZ());
        storage.getCosmos().set(path + ".yaw", location.getYaw());
        storage.getCosmos().set(path + ".pitch", location.getPitch());
    }

    private ConfigurationSection getSection(String id) {
        return storage.getCosmos().getConfigurationSection(PATH + "." + normalize(id));
    }

    private String hologramName(String id) {
        return PREFIX + normalize(id);
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}