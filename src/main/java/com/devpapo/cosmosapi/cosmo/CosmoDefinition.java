package com.devpapo.cosmosapi.cosmo;

import org.bukkit.inventory.ItemStack;

public final class CosmoDefinition {
    private final String id;
    private final String displayName;
    private final CosmosTrigger trigger;
    private final long reward;
    private final long timeIntervalMillis;
    private final boolean enabled;
    private final ItemStack previewIcon;

    public CosmoDefinition(String id, String displayName, CosmosTrigger trigger, long reward, long timeIntervalMillis, boolean enabled, ItemStack previewIcon) {
        this.id = id;
        this.displayName = displayName;
        this.trigger = trigger;
        this.reward = reward;
        this.timeIntervalMillis = timeIntervalMillis;
        this.enabled = enabled;
        this.previewIcon = previewIcon == null ? null : previewIcon.clone();
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

    public ItemStack getPreviewIcon() {
        return previewIcon == null ? null : previewIcon.clone();
    }
}