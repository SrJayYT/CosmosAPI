package com.devpapo.cosmosapi.condition;

import com.devpapo.cosmosapi.cosmo.CosmosTrigger;

public final class ConditionDefinition {
    private final String id;
    private final String cosmoId;
    private final CosmosTrigger trigger;
    private final long amount;

    public ConditionDefinition(String id, String cosmoId, CosmosTrigger trigger, long amount) {
        this.id = id;
        this.cosmoId = cosmoId;
        this.trigger = trigger;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getCosmoId() {
        return cosmoId;
    }

    public CosmosTrigger getTrigger() {
        return trigger;
    }

    public long getAmount() {
        return amount;
    }
}