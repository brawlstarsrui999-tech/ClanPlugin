package ru.clanplugin.glow;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * Палитра доступных цветов свечения для GUI-настройки.
 * Цвета команды (Team) поддерживают только 16 стандартных ChatColor.
 */
public final class GlowColors {

    public static final class GlowColor {
        private final String name;
        private final ChatColor chatColor;
        private final Material icon;
        private final String russian;

        public GlowColor(String name, ChatColor chatColor, Material icon, String russian) {
            this.name = name;
            this.chatColor = chatColor;
            this.icon = icon;
            this.russian = russian;
        }

        public String name() { return name; }
        public ChatColor chatColor() { return chatColor; }
        public Material icon() { return icon; }
        public String russian() { return russian; }
    }

    public static final List<GlowColor> COLORS = Arrays.asList(
            new GlowColor("WHITE", ChatColor.WHITE, Material.WHITE_DYE, "Белый"),
            new GlowColor("GRAY", ChatColor.GRAY, Material.LIGHT_GRAY_DYE, "Светло-серый"),
            new GlowColor("DARK_GRAY", ChatColor.DARK_GRAY, Material.GRAY_DYE, "Тёмно-серый"),
            new GlowColor("BLACK", ChatColor.BLACK, Material.BLACK_DYE, "Чёрный"),
            new GlowColor("BLUE", ChatColor.BLUE, Material.BLUE_DYE, "Синий"),
            new GlowColor("DARK_BLUE", ChatColor.DARK_BLUE, Material.LAPIS_LAZULI, "Тёмно-синий"),
            new GlowColor("AQUA", ChatColor.AQUA, Material.LIGHT_BLUE_DYE, "Голубой"),
            new GlowColor("DARK_AQUA", ChatColor.DARK_AQUA, Material.CYAN_DYE, "Бирюзовый"),
            new GlowColor("GREEN", ChatColor.GREEN, Material.LIME_DYE, "Лаймовый"),
            new GlowColor("DARK_GREEN", ChatColor.DARK_GREEN, Material.GREEN_DYE, "Тёмно-зелёный"),
            new GlowColor("RED", ChatColor.RED, Material.RED_DYE, "Красный"),
            new GlowColor("DARK_RED", ChatColor.DARK_RED, Material.RED_TERRACOTTA, "Тёмно-красный"),
            new GlowColor("GOLD", ChatColor.GOLD, Material.ORANGE_DYE, "Золотой"),
            new GlowColor("YELLOW", ChatColor.YELLOW, Material.YELLOW_DYE, "Жёлтый"),
            new GlowColor("LIGHT_PURPLE", ChatColor.LIGHT_PURPLE, Material.MAGENTA_DYE, "Пурпурный"),
            new GlowColor("DARK_PURPLE", ChatColor.DARK_PURPLE, Material.PURPLE_DYE, "Фиолетовый")
    );

    /** Слоты (в порядке {@link #COLORS}) в меню выбора цвета. */
    public static final int[] COLOR_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29
    };

    /** Слот кнопки "Выключить свечение" в меню выбора цвета. */
    public static final int OFF_SLOT = 31;

    /** Слот кнопки "Назад" в меню выбора цвета. */
    public static final int BACK_SLOT = 45;

    private GlowColors() {}

    public static GlowColor byName(String name) {
        if (name == null) return null;
        for (GlowColor color : COLORS) {
            if (color.name().equalsIgnoreCase(name)) return color;
        }
        return null;
    }

    public static GlowColor bySlot(int slot) {
        for (int i = 0; i < COLOR_SLOTS.length && i < COLORS.size(); i++) {
            if (COLOR_SLOTS[i] == slot) return COLORS.get(i);
        }
        return null;
    }

    /** Русское название цвета по его сохранённому имени (для лора/сообщений). */
    public static String russian(String colorName) {
        GlowColor color = byName(colorName);
        return color != null ? color.russian() : colorName;
    }
}