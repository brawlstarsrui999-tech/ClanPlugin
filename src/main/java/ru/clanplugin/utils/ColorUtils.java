package ru.clanplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class ColorUtils {

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static Component component(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    public static String strip(String msg) {
        return ChatColor.stripColor(color(msg));
    }
}
