package com.devpapo.cosmosapi.economy;

import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import me.gypopo.economyshopgui.api.objects.ExternalEconomy;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public final class CosmosExternalEconomy extends ExternalEconomy {
    private final CosmosService cosmosService;
    private final String cosmoId;
    private final String name;

    public CosmosExternalEconomy(CosmosService cosmosService, String cosmoId) {
        this(cosmosService, "CosmosAPI_" + cosmoId, cosmoId);
    }

    public CosmosExternalEconomy(CosmosService cosmosService, String name, String cosmoId) {
        this.cosmosService = cosmosService;
        this.name = name;
        this.cosmoId = cosmoId.toLowerCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return name;
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
        return cosmoId;
    }

    @Override
    public boolean isDecimal() {
        return false;
    }

    @Override
    public String formatPrice(double amount) {
        long wholeAmount = Double.isFinite(amount) && amount >= 0D && amount <= Long.MAX_VALUE
                ? (long) amount
                : 0L;
        return NumberFormatUtil.format(wholeAmount) + " " + getFriendly();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        CosmoDefinition cosmo = cosmosService.getCosmo(cosmoId);
        if (player == null || cosmo == null || !cosmo.isEnabled()) {
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
        if (!Double.isFinite(amount) || amount <= 0D || amount > Long.MAX_VALUE || amount != Math.rint(amount)) {
            return 0L;
        }
        return (long) amount;
    }
}