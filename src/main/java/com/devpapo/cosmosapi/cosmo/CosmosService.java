package com.devpapo.cosmosapi.cosmo;

import com.devpapo.cosmosapi.storage.CosmosStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class CosmosService {
    private final CosmosStorage storage;
    private final Map<String, CosmoDefinition> cosmos = new HashMap<>();

    public CosmosService(CosmosStorage storage) {
        this.storage = storage;
        reload();
    }

    public void reload() {
        cosmos.clear();
        for (String id : storage.getCosmoIds()) {
            ConfigurationSection section = storage.getCosmos().getConfigurationSection("cosmos." + id);
            if (section == null) {
                continue;
            }
            CosmosTrigger trigger = CosmosTrigger.fromInput(section.getString("type", ""));
            long reward = section.getLong("reward", 0L);
            if (trigger != null && reward > 0L) {
                long interval = 60_000L;
                if (trigger == CosmosTrigger.TIME) {
                    TimeRewardUnit unit = TimeRewardUnit.fromInput(section.getString("unit", "MINUTE"));
                    long amount = Math.max(1L, section.getLong("interval", 1L));
                    if (unit == null) {
                        continue;
                    }
                    try {
                        interval = unit.toMilliseconds(amount);
                    } catch (ArithmeticException ignored) {
                        continue;
                    }
                }
                cosmos.put(normalize(id), new CosmoDefinition(id, section.getString("display-name", id), trigger, reward, interval, section.getBoolean("enabled", true), section.getItemStack("preview-icon")));
            }
        }
    }

    public boolean create(String id, CosmosTrigger trigger, long reward, long interval, TimeRewardUnit unit) {
        String key = normalize(id);
        if (cosmos.containsKey(key) || trigger == null || reward <= 0L || (trigger == CosmosTrigger.TIME && (interval <= 0L || unit == null))) {
            return false;
        }
        long timeIntervalMillis = 60_000L;
        if (trigger == CosmosTrigger.TIME) {
            try {
                timeIntervalMillis = unit.toMilliseconds(interval);
            } catch (ArithmeticException exception) {
                return false;
            }
        }
        String path = "cosmos." + key;
        storage.getCosmos().set(path + ".display-name", "&d" + id);
        storage.getCosmos().set(path + ".type", trigger.name());
        storage.getCosmos().set(path + ".reward", reward);
        storage.getCosmos().set(path + ".enabled", true);
        if (trigger == CosmosTrigger.TIME) {
            storage.getCosmos().set(path + ".interval", interval);
            storage.getCosmos().set(path + ".unit", unit.name());
        }
        storage.saveCosmos();
        cosmos.put(key, new CosmoDefinition(key, "&d" + id, trigger, reward, timeIntervalMillis, true, null));
        return true;
    }

    public boolean delete(String id) {
        String key = normalize(id);
        if (!cosmos.containsKey(key)) {
            return false;
        }
        storage.getCosmos().set("cosmos." + key, null);
        storage.saveCosmos();
        cosmos.remove(key);
        return true;
    }

    public boolean setDisplayName(String id, String displayName) {
        CosmoDefinition definition = getCosmo(id);
        if (definition == null) {
            return false;
        }
        storage.getCosmos().set("cosmos." + definition.getId() + ".display-name", displayName);
        storage.saveCosmos();
        cosmos.put(normalize(definition.getId()), new CosmoDefinition(definition.getId(), displayName, definition.getTrigger(), definition.getReward(), definition.getTimeIntervalMillis(), definition.isEnabled(), definition.getPreviewIcon()));
        return true;
    }

    public boolean setTrigger(String id, CosmosTrigger trigger) {
        CosmoDefinition definition = getCosmo(id);
        if (definition == null || trigger == null) {
            return false;
        }
        String path = "cosmos." + definition.getId();
        storage.getCosmos().set(path + ".type", trigger.name());
        long intervalMillis = 60_000L;
        if (trigger == CosmosTrigger.TIME) {
            storage.getCosmos().set(path + ".interval", 1L);
            storage.getCosmos().set(path + ".unit", TimeRewardUnit.MINUTE.name());
        } else {
            storage.getCosmos().set(path + ".interval", null);
            storage.getCosmos().set(path + ".unit", null);
        }
        storage.saveCosmos();
        cosmos.put(normalize(definition.getId()), new CosmoDefinition(definition.getId(), definition.getDisplayName(), trigger, definition.getReward(), intervalMillis, definition.isEnabled(), definition.getPreviewIcon()));
        return true;
    }

    public boolean setReward(String id, long reward) {
        CosmoDefinition definition = getCosmo(id);
        if (definition == null || reward <= 0L) {
            return false;
        }
        storage.getCosmos().set("cosmos." + definition.getId() + ".reward", reward);
        storage.saveCosmos();
        cosmos.put(normalize(definition.getId()), new CosmoDefinition(definition.getId(), definition.getDisplayName(), definition.getTrigger(), reward, definition.getTimeIntervalMillis(), definition.isEnabled(), definition.getPreviewIcon()));
        return true;
    }

    public boolean setTimeInterval(String id, long interval, TimeRewardUnit unit) {
        CosmoDefinition definition = getCosmo(id);
        if (definition == null || definition.getTrigger() != CosmosTrigger.TIME || interval <= 0L || unit == null) {
            return false;
        }
        long intervalMillis;
        try {
            intervalMillis = unit.toMilliseconds(interval);
        } catch (ArithmeticException exception) {
            return false;
        }
        String path = "cosmos." + definition.getId();
        storage.getCosmos().set(path + ".interval", interval);
        storage.getCosmos().set(path + ".unit", unit.name());
        storage.saveCosmos();
        cosmos.put(normalize(definition.getId()), new CosmoDefinition(definition.getId(), definition.getDisplayName(), definition.getTrigger(), definition.getReward(), intervalMillis, definition.isEnabled(), definition.getPreviewIcon()));
        return true;
    }

    public boolean setEnabled(String id, boolean enabled) {
        CosmoDefinition definition = getCosmo(id);
        if (definition == null) {
            return false;
        }
        storage.getCosmos().set("cosmos." + definition.getId() + ".enabled", enabled);
        storage.saveCosmos();
        cosmos.put(normalize(definition.getId()), new CosmoDefinition(definition.getId(), definition.getDisplayName(), definition.getTrigger(), definition.getReward(), definition.getTimeIntervalMillis(), enabled, definition.getPreviewIcon()));
        return true;
    }

    public boolean setPreviewIcon(String id, ItemStack previewIcon) {
        CosmoDefinition definition = getCosmo(id);
        if (definition == null || previewIcon == null || previewIcon.getType().isAir()) {
            return false;
        }
        ItemStack savedIcon = previewIcon.clone();
        savedIcon.setAmount(1);
        storage.getCosmos().set("cosmos." + definition.getId() + ".preview-icon", savedIcon);
        storage.saveCosmos();
        cosmos.put(normalize(definition.getId()), new CosmoDefinition(definition.getId(), definition.getDisplayName(), definition.getTrigger(), definition.getReward(), definition.getTimeIntervalMillis(), definition.isEnabled(), savedIcon));
        return true;
    }

    public CosmoDefinition getCosmo(String id) {
        return id == null ? null : cosmos.get(normalize(id));
    }

    public List<CosmoDefinition> getCosmos() {
        List<CosmoDefinition> definitions = new ArrayList<>(cosmos.values());
        definitions.sort(Comparator.comparing(CosmoDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        return definitions;
    }

    public long getBalance(UUID playerId, String cosmoId) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        if (cosmo == null) {
            return 0L;
        }
        return storage.getPlayers().getLong("players." + playerId + ".balances." + cosmo.getId(), 0L);
    }

    public void setBalance(UUID playerId, String cosmoId, long amount) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        if (cosmo == null) {
            return;
        }
        storage.getPlayers().set("players." + playerId + ".balances." + cosmo.getId(), amount);
        storage.savePlayers();
    }

    public void deposit(UUID playerId, String cosmoId, long amount) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        if (amount > 0L && cosmo != null && cosmo.isEnabled()) {
            setBalance(playerId, cosmoId, addSafely(getBalance(playerId, cosmoId), amount));
        }
    }

    public void adjustBalance(UUID playerId, String cosmoId, long amount) {
        if (getCosmo(cosmoId) != null) {
            setBalance(playerId, cosmoId, addSafely(getBalance(playerId, cosmoId), amount));
        }
    }

    public boolean withdraw(UUID playerId, String cosmoId, long amount) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        if (amount <= 0L || cosmo == null || !cosmo.isEnabled()) {
            return false;
        }
        long balance = getBalance(playerId, cosmoId);
        if (balance < amount) {
            return false;
        }
        setBalance(playerId, cosmoId, balance - amount);
        return true;
    }

    public long withdrawUpTo(UUID playerId, String cosmoId, long amount) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        if (amount <= 0L || cosmo == null || !cosmo.isEnabled()) {
            return 0L;
        }
        long withdrawn = Math.min(getBalance(playerId, cosmoId), amount);
        if (withdrawn > 0L) {
            setBalance(playerId, cosmoId, getBalance(playerId, cosmoId) - withdrawn);
        }
        return withdrawn;
    }

    public boolean transfer(UUID senderId, UUID recipientId, String cosmoId, long amount) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        if (amount <= 0L || senderId.equals(recipientId) || cosmo == null || !cosmo.isEnabled()) {
            return false;
        }
        long senderBalance = getBalance(senderId, cosmoId);
        if (senderBalance < amount) {
            return false;
        }
        long recipientBalance = getBalance(recipientId, cosmoId);
        if (recipientBalance > Long.MAX_VALUE - amount) {
            return false;
        }
        storage.getPlayers().set("players." + senderId + ".balances." + cosmo.getId(), senderBalance - amount);
        storage.getPlayers().set("players." + recipientId + ".balances." + cosmo.getId(), recipientBalance + amount);
        storage.savePlayers();
        return true;
    }

    public List<Map.Entry<UUID, Long>> getTop(String cosmoId, int limit) {
        return getTop(cosmoId, 0, limit);
    }

    public List<Map.Entry<UUID, Long>> getTop(String cosmoId, int offset, int limit) {
        CosmoDefinition cosmo = getCosmo(cosmoId);
        List<Map.Entry<UUID, Long>> balances = new ArrayList<>();
        if (cosmo == null || limit <= 0 || storage.getPlayers().getConfigurationSection("players") == null) {
            return balances;
        }
        for (String playerId : storage.getPlayers().getConfigurationSection("players").getKeys(false)) {
            try {
                long balance = storage.getPlayers().getLong("players." + playerId + ".balances." + cosmo.getId(), 0L);
                if (balance > 0L) {
                    balances.add(Map.entry(UUID.fromString(playerId), balance));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        balances.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        int from = Math.min(Math.max(0, offset), balances.size());
        return balances.subList(from, Math.min(from + limit, balances.size()));
    }

    public void reward(UUID playerId, CosmosTrigger trigger) {
        for (CosmoDefinition definition : cosmos.values()) {
            if (definition.isEnabled() && definition.getTrigger() == trigger) {
                deposit(playerId, definition.getId(), definition.getReward());
            }
        }
    }

    public void rewardTime(UUID playerId) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (CosmoDefinition definition : cosmos.values()) {
            if (!definition.isEnabled() || definition.getTrigger() != CosmosTrigger.TIME) {
                continue;
            }
            String path = "players." + playerId + ".last-time-rewards." + definition.getId();
            long lastReward = storage.getPlayers().getLong(path, 0L);
            if (now - lastReward >= definition.getTimeIntervalMillis()) {
                long balance = getBalance(playerId, definition.getId());
                storage.getPlayers().set("players." + playerId + ".balances." + definition.getId(), addSafely(balance, definition.getReward()));
                storage.getPlayers().set(path, now);
                changed = true;
            }
        }
        if (changed) {
            storage.savePlayers();
        }
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    private long addSafely(long current, long amount) {
        if (amount > 0L && current > Long.MAX_VALUE - amount) {
            return Long.MAX_VALUE;
        }
        if (amount < 0L && current < Long.MIN_VALUE - amount) {
            return Long.MIN_VALUE;
        }
        return current + amount;
    }
}