package com.devpapo.cosmosapi.util;

import com.devpapo.cosmosapi.CosmosAPI;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class MessageManager {
    private final CosmosAPI plugin;
    private FileConfiguration messages;

    public MessageManager(CosmosAPI plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        sender.sendMessage(ColorUtil.color(format("prefix", Map.of()) + format(key, replacements)));
    }

    public void sendWithoutPrefix(CommandSender sender, String key, Map<String, String> replacements) {
        sender.sendMessage(ColorUtil.color(format(key, replacements)));
    }

    public String format(String key, Map<String, String> replacements) {
        String message = messages.getString(key, "");
        return applyReplacements(message, replacements);
    }

    public List<String> formatList(String key, Map<String, String> replacements) {
        List<String> formatted = new ArrayList<>();
        for (String line : messages.getStringList(key)) {
            formatted.add(applyReplacements(line, replacements));
        }
        return formatted;
    }

    private String applyReplacements(String message, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}