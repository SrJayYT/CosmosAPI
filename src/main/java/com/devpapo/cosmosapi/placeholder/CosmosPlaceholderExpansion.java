package com.devpapo.cosmosapi.placeholder;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import java.util.stream.Collectors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class CosmosPlaceholderExpansion extends PlaceholderExpansion {
    private final CosmosAPI plugin;
    private final CosmosService cosmosService;
    private final String identifier;

    public CosmosPlaceholderExpansion(CosmosAPI plugin, CosmosService cosmosService, String identifier) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("cosmos")) {
            return cosmosService.getCosmos().stream().map(CosmoDefinition::getId).collect(Collectors.joining(", "));
        }
        if (params.toLowerCase().endsWith("_displayname")) {
            String id = params.substring(0, params.length() - "_displayname".length());
            CosmoDefinition cosmo = cosmosService.getCosmo(id);
            return cosmo == null ? null : ColorUtil.color(cosmo.getDisplayName());
        }
        CosmoDefinition cosmo = cosmosService.getCosmo(params);
        if (cosmo == null) {
            return null;
        }
        return player == null ? "0" : NumberFormatUtil.format(cosmosService.getBalance(player.getUniqueId(), cosmo.getId()));
    }
}