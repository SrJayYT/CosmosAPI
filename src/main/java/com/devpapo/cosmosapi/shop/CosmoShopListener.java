package com.devpapo.cosmosapi.shop;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class CosmoShopListener implements Listener {
    private final CosmoShopManager shopManager;

    public CosmoShopListener(CosmoShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        shopManager.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        shopManager.handleDrag(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        shopManager.handleClose(event.getInventory());
    }
}