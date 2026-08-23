package com.devpapo.cosmosapi.economy;

import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import me.gypopo.economyshopgui.api.events.EconomyPreLoadEvent;
import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.Map;

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

    @EventHandler(ignoreCancelled = true)
    public void onPreTransaction(PreTransactionEvent event) {
        if (Transaction.Mode.getFromType(event.getTransactionType()) != Transaction.Mode.BUY) {
            return;
        }
        Map<EcoType, Double> prices = event.getPrices();
        if (prices.isEmpty()) {
            if (!hasEnoughCosmos(event.getPlayer().getUniqueId(), event.getShopItem().getEcoType(), event.getPrice())) {
                event.setCancelled(true);
            }
            return;
        }
        for (Map.Entry<EcoType, Double> price : prices.entrySet()) {
            if (!hasEnoughCosmos(event.getPlayer().getUniqueId(), price.getKey(), price.getValue())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean hasEnoughCosmos(java.util.UUID playerId, EcoType economy, double price) {
        if (economy.getType() != EconomyType.EXTERNAL || price < 0D) {
            return true;
        }
        String currency = economy.getCurrency();
        String prefix = "CosmosAPI_";
        if (currency == null || !currency.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return true;
        }
        String cosmoId = currency.substring(prefix.length()).toLowerCase(Locale.ROOT);
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        if (cosmo == null) {
            return false;
        }
        if (!Double.isFinite(price) || price > Long.MAX_VALUE) {
            return false;
        }
        long amount = (long) Math.ceil(price);
        return amount <= 0L || cosmosService.getBalance(playerId, cosmo.getId()) >= amount;
    }
}