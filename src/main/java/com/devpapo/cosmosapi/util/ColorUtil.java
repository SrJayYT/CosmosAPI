package com.devpapo.cosmosapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public final class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&#([0-9a-f]{6})");

    private ColorUtil() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(ChatColor.of("#" + matcher.group(1)).toString()));
        }
        matcher.appendTail(result);
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    public static List<String> color(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(color(line));
        }
        return colored;
    }
}