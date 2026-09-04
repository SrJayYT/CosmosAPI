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
            if (cosmo.getId().equalsIgnoreCase("cosmo")) {
                event.registerExternal(new CosmosExternalEconomy(cosmosService, "CosmosAPI", cosmo.getId()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreTransaction(PreTransactionEvent event) {
        boolean isBuy = Transaction.Mode.getFromType(event.getTransactionType()) == Transaction.Mode.BUY;
        Map<EcoType, Double> prices = event.getPrices();
        if (prices.isEmpty()) {
            if (!isValidCosmosTransaction(event.getPlayer().getUniqueId(), event.getShopItem().getEcoType(), event.getPrice(), isBuy)) {
                event.setCancelled(true);
            }
            return;
        }
        for (Map.Entry<EcoType, Double> price : prices.entrySet()) {
            if (!isValidCosmosTransaction(event.getPlayer().getUniqueId(), price.getKey(), price.getValue(), isBuy)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isValidCosmosTransaction(java.util.UUID playerId, EcoType economy, double price, boolean isBuy) {
        if (economy.getType() != EconomyType.EXTERNAL) {
            return true;
        }
        String currency = economy.getCurrency();
        String prefix = "CosmosAPI_";
        String cosmoId;
        if (currency != null && currency.equalsIgnoreCase("CosmosAPI")) {
            cosmoId = "cosmo";
        } else if (currency != null && currency.regionMatches(true, 0, prefix, 0, prefix.length())) {
            cosmoId = currency.substring(prefix.length()).toLowerCase(Locale.ROOT);
        } else {
            return true;
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        if (cosmo == null || !cosmo.isEnabled()) {
            return false;
        }
        if (!Double.isFinite(price) || price < 0D || price > Long.MAX_VALUE || price != Math.rint(price)) {
            return false;
        }
        long amount = (long) price;
        return !isBuy || amount == 0L || cosmosService.getBalance(playerId, cosmo.getId()) >= amount;
    }
}