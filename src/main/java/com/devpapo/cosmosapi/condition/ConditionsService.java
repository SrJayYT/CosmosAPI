package com.devpapo.cosmosapi.condition;

import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmosTrigger;
import com.devpapo.cosmosapi.storage.CosmosStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;

public final class ConditionsService {
    private final CosmosStorage storage;
    private final CosmosService cosmosService;
    private final Map<String, ConditionDefinition> conditions = new HashMap<>();

    public ConditionsService(CosmosStorage storage, CosmosService cosmosService) {
        this.storage = storage;
        this.cosmosService = cosmosService;
        reload();
    }

    public void reload() {
        conditions.clear();
        for (String id : storage.getConditionIds()) {
            ConfigurationSection section = storage.getConditions().getConfigurationSection("conditions." + id);
            if (section == null) {
                continue;
            }
            CosmosTrigger trigger = CosmosTrigger.fromInput(section.getString("type", ""));
            String cosmoId = section.getString("cosmo", "");
            long amount = section.getLong("amount", 0L);
            if (isSupportedTrigger(trigger) && cosmosService.getCosmo(cosmoId) != null && amount > 0L) {
                conditions.put(normalize(id), new ConditionDefinition(id, cosmosService.getCosmo(cosmoId).getId(), trigger, amount));
            }
        }
    }

    public boolean create(String id, String cosmoId, CosmosTrigger trigger, long amount) {
        String key = normalize(id);
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        if (conditions.containsKey(key) || cosmo == null || !isSupportedTrigger(trigger) || amount <= 0L) {
            return false;
        }
        String path = "conditions." + key;
        storage.getConditions().set(path + ".cosmo", cosmo.getId());
        storage.getConditions().set(path + ".type", trigger.name());
        storage.getConditions().set(path + ".amount", amount);
        storage.saveConditions();
        conditions.put(key, new ConditionDefinition(key, cosmo.getId(), trigger, amount));
        return true;
    }

    public boolean delete(String id) {
        ConditionDefinition definition = getCondition(id);
        if (definition == null) {
            return false;
        }
        storage.getConditions().set("conditions." + definition.getId(), null);
        storage.saveConditions();
        conditions.remove(normalize(definition.getId()));
        return true;
    }

    public boolean setCosmo(String id, String cosmoId) {
        ConditionDefinition definition = getCondition(id);
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        if (definition == null || cosmo == null) {
            return false;
        }
        storage.getConditions().set("conditions." + definition.getId() + ".cosmo", cosmo.getId());
        storage.saveConditions();
        conditions.put(normalize(definition.getId()), new ConditionDefinition(definition.getId(), cosmo.getId(), definition.getTrigger(), definition.getAmount()));
        return true;
    }

    public boolean setTrigger(String id, CosmosTrigger trigger) {
        ConditionDefinition definition = getCondition(id);
        if (definition == null || !isSupportedTrigger(trigger)) {
            return false;
        }
        storage.getConditions().set("conditions." + definition.getId() + ".type", trigger.name());
        storage.saveConditions();
        conditions.put(normalize(definition.getId()), new ConditionDefinition(definition.getId(), definition.getCosmoId(), trigger, definition.getAmount()));
        return true;
    }

    public boolean setAmount(String id, long amount) {
        ConditionDefinition definition = getCondition(id);
        if (definition == null || amount <= 0L) {
            return false;
        }
        storage.getConditions().set("conditions." + definition.getId() + ".amount", amount);
        storage.saveConditions();
        conditions.put(normalize(definition.getId()), new ConditionDefinition(definition.getId(), definition.getCosmoId(), definition.getTrigger(), amount));
        return true;
    }

    public ConditionDefinition getCondition(String id) {
        return id == null ? null : conditions.get(normalize(id));
    }

    public List<ConditionDefinition> getConditions() {
        List<ConditionDefinition> definitions = new ArrayList<>(conditions.values());
        definitions.sort(Comparator.comparing(ConditionDefinition::getId, String.CASE_INSENSITIVE_ORDER));
        return definitions;
    }

    public void apply(UUID playerId, CosmosTrigger trigger) {
        for (ConditionDefinition definition : conditions.values()) {
            if (definition.getTrigger() == trigger && cosmosService.getCosmo(definition.getCosmoId()) != null && cosmosService.getCosmo(definition.getCosmoId()).isEnabled()) {
                cosmosService.adjustBalance(playerId, definition.getCosmoId(), -definition.getAmount());
            }
        }
    }

    public boolean isSupportedTrigger(CosmosTrigger trigger) {
        return trigger != null && trigger != CosmosTrigger.TIME;
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}