package com.devpapo.cosmosapi.economy;

import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import me.gypopo.economyshopgui.api.objects.ExternalEconomy;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public final class CosmosExternalEconomy extends ExternalEconomy {
    private final CosmosService cosmosService;
    private final String cosmoId;

    public CosmosExternalEconomy(CosmosService cosmosService, String cosmoId) {
        this.cosmosService = cosmosService;
        this.cosmoId = cosmoId.toLowerCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return "CosmosAPI_" + cosmoId;
    }

    @Override
    public String getSingular() {
        return cosmoId;
    }

    @Override
    public String getPlural() {
        return cosmoId;
    }

    @Override
    public String getFriendly() {
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        return cosmo == null ? cosmoId : cosmo.getDisplayName();
    }

    @Override
    public boolean isDecimal() {
        return false;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) {
            return 0D;
        }
        return cosmosService.getBalance(player.getUniqueId(), cosmoId);
    }

    @Override
    public void depositBalance(OfflinePlayer player, double amount) {
        long wholeAmount = toWholeAmount(amount);
        if (player != null && wholeAmount > 0L) {
            cosmosService.deposit(player.getUniqueId(), cosmoId, wholeAmount);
        }
    }

    @Override
    public void withdrawBalance(OfflinePlayer player, double amount) {
        long wholeAmount = toWholeAmount(amount);
        if (player != null && wholeAmount > 0L) {
            cosmosService.withdraw(player.getUniqueId(), cosmoId, wholeAmount);
        }
    }

    private long toWholeAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0D || amount > Long.MAX_VALUE) {
            return 0L;
        }
        return (long) Math.ceil(amount);
    }
}