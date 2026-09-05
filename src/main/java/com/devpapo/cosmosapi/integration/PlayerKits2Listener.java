package com.devpapo.cosmosapi.integration;

import com.devpapo.cosmosapi.CosmosAPI;
import com.devpapo.cosmosapi.cosmo.CosmoDefinition;
import com.devpapo.cosmosapi.cosmo.CosmosService;
import com.devpapo.cosmosapi.util.ColorUtil;
import com.devpapo.cosmosapi.util.NumberFormatUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class PlayerKits2Listener implements Listener {
    private final CosmosAPI plugin;
    private final CosmosService cosmosService;

    public PlayerKits2Listener(CosmosAPI plugin, CosmosService cosmosService) {
        this.plugin = plugin;
        this.cosmosService = cosmosService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onKitCommand(PlayerCommandPreprocessEvent event) {
        String[] arguments = event.getMessage().trim().split("\\s+");
        if (arguments.length < 2 || !isKitCommand(arguments[0])) {
            return;
        }
        String kitName = arguments[1].equalsIgnoreCase("claim") && arguments.length >= 3 ? arguments[2] : arguments[1];
        KitPrice price = getKitPrice(kitName);
        if (price == null) {
            return;
        }
        event.setCancelled(true);
        purchase(event.getPlayer(), price);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onKitClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        String kitName = getKitName(item);
        KitPrice price = getKitPrice(kitName);
        if (price == null) {
            return;
        }
        event.setCancelled(true);
        event.setCurrentItem(null);
        Bukkit.getScheduler().runTask(plugin, () -> {
            event.getView().getTopInventory().setItem(event.getSlot(), item);
            purchase((Player) event.getWhoClicked(), price);
        });
    }

    private boolean isKitCommand(String label) {
        String normalized = label.startsWith("/") ? label.substring(1).toLowerCase(Locale.ROOT) : label.toLowerCase(Locale.ROOT);
        return normalized.equals("kit") || normalized.equals("kits") || normalized.equals("playerkits");
    }

    private String getKitName(ItemStack item) {
        Plugin playerKits = Bukkit.getPluginManager().getPlugin("PlayerKits2");
        if (playerKits == null) {
            return null;
        }
        try {
            ClassLoader classLoader = playerKits.getClass().getClassLoader();
            Class<?> playerKitsClass = Class.forName("pk.ajneb97.PlayerKits2", true, classLoader);
            Class<?> itemUtilsClass = Class.forName("pk.ajneb97.utils.ItemUtils", true, classLoader);
            Method getTag = itemUtilsClass.getMethod("getTagStringItem", playerKitsClass, ItemStack.class, String.class);
            Object value = getTag.invoke(null, playerKits, item, "playerkits_kit");
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private KitPrice getKitPrice(String kitName) {
        if (!plugin.getConfig().getBoolean("playerkits2.enabled", false) || kitName == null) {
            return null;
        }
        ConfigurationSection kits = plugin.getConfig().getConfigurationSection("playerkits2.kits");
        if (kits == null) {
            return null;
        }
        for (String configuredKit : kits.getKeys(false)) {
            if (!configuredKit.equalsIgnoreCase(kitName)) {
                continue;
            }
            ConfigurationSection section = kits.getConfigurationSection(configuredKit);
            if (section == null) {
                return null;
            }
            String cosmoId = section.getString("cosmo", "");
            long amount = section.getLong("price", 0L);
            if (cosmoId.isBlank() || amount <= 0L) {
                return null;
            }
            return new KitPrice(configuredKit, cosmoId, amount);
        }
        return null;
    }

    private void purchase(Player player, KitPrice price) {
        CosmoDefinition cosmo = cosmosService.getCosmo(price.cosmoId());
        if (cosmo == null || !cosmo.isEnabled()) {
            plugin.getMessageManager().send(player, "unknown-cosmo", Map.of("cosmo", price.cosmoId()));
            return;
        }
        if (cosmosService.getBalance(player.getUniqueId(), cosmo.getId()) < price.amount()) {
            plugin.getMessageManager().send(player, "insufficient-funds", Map.of("cosmo", cosmo.getDisplayName()));
            return;
        }
        if (!cosmosService.withdraw(player.getUniqueId(), cosmo.getId(), price.amount())) {
            plugin.getMessageManager().send(player, "playerkits-cosmo-error", Map.of());
            return;
        }
        KitClaimResult result = claimKit(player, price.kitName());
        if (result == null) {
            cosmosService.deposit(player.getUniqueId(), cosmo.getId(), price.amount());
            plugin.getMessageManager().send(player, "playerkits-cosmo-error", Map.of());
            return;
        }
        if (result.errorMessage() != null) {
            cosmosService.deposit(player.getUniqueId(), cosmo.getId(), price.amount());
            player.sendMessage(ColorUtil.color(result.errorMessage()));
            return;
        }
        if (result.proceedToBuy()) {
            cosmosService.deposit(player.getUniqueId(), cosmo.getId(), price.amount());
            plugin.getMessageManager().send(player, "playerkits-cosmo-requirements", Map.of());
            return;
        }
        plugin.getMessageManager().send(player, "playerkits-cosmo-success", Map.of(
                "kit", price.kitName(),
                "price", NumberFormatUtil.format(price.amount()),
                "cosmo", cosmo.getDisplayName()
        ));
    }

    private KitClaimResult claimKit(Player player, String kitName) {
        Plugin playerKits = Bukkit.getPluginManager().getPlugin("PlayerKits2");
        if (playerKits == null) {
            return null;
        }
        try {
            ClassLoader classLoader = playerKits.getClass().getClassLoader();
            Class<?> playerKitsClass = Class.forName("pk.ajneb97.PlayerKits2", true, classLoader);
            Class<?> instructionsClass = Class.forName("pk.ajneb97.model.internal.GiveKitInstructions", true, classLoader);
            Object manager = playerKitsClass.getMethod("getKitsManager").invoke(playerKits);
            Object instructions = instructionsClass.getConstructor().newInstance();
            Method giveKit = manager.getClass().getMethod("giveKit", Player.class, String.class, instructionsClass);
            Object result = giveKit.invoke(manager, player, kitName, instructions);
            Class<?> resultClass = result.getClass();
            if ((boolean) resultClass.getMethod("isError").invoke(result)) {
                return new KitClaimResult((String) resultClass.getMethod("getMessage").invoke(result), false);
            }
            return new KitClaimResult(null, (boolean) resultClass.getMethod("isProceedToBuy").invoke(result));
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException exception) {
            return null;
        } catch (InvocationTargetException exception) {
            plugin.getLogger().warning("PlayerKits2 no pudo entregar un kit con cosmos: " + exception.getCause().getClass().getSimpleName());
            return null;
        }
    }

    private record KitPrice(String kitName, String cosmoId, long amount) {
    }

    private record KitClaimResult(String errorMessage, boolean proceedToBuy) {
    }
}