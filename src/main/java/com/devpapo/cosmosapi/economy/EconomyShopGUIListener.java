package com.devpapo.cosmosapi.economy;

import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import me.gypopo.economyshopgui.api.events.EconomyPreLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class EconomyShopGUIListener implements Listener {
    private final CosmosService cosmosService;

    public EconomyShopGUIListener(CosmosService cosmosService) {
        this.cosmosService = cosmosService;
    }

    @EventHandler
    public void onEconomyPreLoad(EconomyPreLoadEvent event) {
        for (CosmoDefinition cosmo : cosmosService.getCosmos()) {
            event.registerExternal(new CosmosExternalEconomy(cosmosService, cosmo.getId()));
        }
    }
}