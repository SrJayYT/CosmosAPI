package com.devpapo.cosmosapi.cosmo;

public final class CosmoDefinition {
    private final String id;
    private final String displayName;
    private final CosmosTrigger trigger;
    private final long reward;
    private final long timeIntervalMillis;
    private final boolean enabled;

    public CosmoDefinition(String id, String displayName, CosmosTrigger trigger, long reward, long timeIntervalMillis, boolean enabled) {
        this.id = id;
        this.displayName = displayName;
        this.trigger = trigger;
        this.reward = reward;
        this.timeIntervalMillis = timeIntervalMillis;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CosmosTrigger getTrigger() {
        return trigger;
    }

    public long getReward() {
        return reward;
    }

    public long getTimeIntervalMillis() {
        return timeIntervalMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }
}