package com.devpapo.cosmosapi.menu;

import org.bukkit.inventory.ItemStack;

public final class MenuItem {
    private final ItemStack item;
    private final String cosmoId;
    private final long price;

    public MenuItem(ItemStack item, String cosmoId, long price) {
        this.item = item;
        this.cosmoId = cosmoId;
        this.price = price;
    }

    public ItemStack getItem() {
        return item;
    }

    public String getCosmoId() {
        return cosmoId;
    }

    public long getPrice() {
        return price;
    }
}