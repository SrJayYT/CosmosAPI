package com.devpapo.cosmosapi.listener;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmosTrigger;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class CosmoRewardListener implements Listener {
    private final CosmosAPI plugin;
    private final CosmosService cosmosService;

    public CosmoRewardListener(CosmosAPI plugin, CosmosService cosmosService) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        cosmosService.reward(event.getPlayer().getUniqueId(), CosmosTrigger.BLOCK_BREAK);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        cosmosService.reward(event.getPlayer().getUniqueId(), CosmosTrigger.BLOCK_PLACE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        cosmosService.reward(event.getEntity().getUniqueId(), CosmosTrigger.PLAYER_DEATH);
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            cosmosService.reward(killer.getUniqueId(), CosmosTrigger.PLAYER_KILL);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (event.getEntity() instanceof Tameable tameable && tameable.isTamed()) {
            AnimalTamer owner = tameable.getOwner();
            if (owner instanceof Player player) {
                cosmosService.reward(player.getUniqueId(), CosmosTrigger.TAMED_ANIMAL_DEATH);
            }
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            cosmosService.reward(killer.getUniqueId(), CosmosTrigger.MOB_KILL);
        }
    }
}