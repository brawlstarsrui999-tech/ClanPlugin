package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

import java.text.DecimalFormat;

public class ClanUpgradesMenu {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        boolean isOwner = clan.isOwner(player.getUniqueId());
        boolean canUpgradeShop = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_SHOP);
        boolean canUpgradeFeatures = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_FEATURES);

        Inventory inv = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8⬆ &aУлучшения клана"));

        fillBorder(inv);

        // Header info
        inv.setItem(4, new ItemBuilder(Material.BOOK)
                .name("&6&l💰 Казна клана")
                .lore("&7Баланс: &6" + FORMAT.format((int) clan.getBalance()) + " монет",
                        "&7Все улучшения покупаются из казны")
                .build());

        // ===== SHOP UPGRADES =====
        // Shop level 2
        boolean hasShopL2 = clan.getShopLevel() >= 2;
        long shopL2Cost = 60000;
        inv.setItem(10, buildShopUpgradeItem(clan, 2, shopL2Cost, hasShopL2, canUpgradeShop));

        // Shop level 3
        boolean hasShopL3 = clan.getShopLevel() >= 3;
        long shopL3Cost = 130000;
        inv.setItem(11, buildShopUpgradeItem(clan, 3, shopL3Cost, hasShopL3, canUpgradeShop));

        // Shop level 4
        boolean hasShopL4 = clan.getShopLevel() >= 4;
        long shopL4Cost = 250000;
        inv.setItem(12, buildShopUpgradeItem(clan, 4, shopL4Cost, hasShopL4, canUpgradeShop));

        // ===== FEATURE UPGRADES =====
        // Clan TP
        boolean hasClanTp = clan.hasFeature("clan-tp");
        long tpCost = plugin.getConfig().getLong("upgrades.features.clan-tp", 25000);
        inv.setItem(19, buildFeatureItem(clan, "clan-tp", "⚡ Телепортация /clan tp",
                new String[]{
                        "&7Телепортируйтесь к любому",
                        "&7участнику клана",
                        "&7Команда: &e/clan tp <игрок>",
                        "",
                        "&7Стоимость: &6" + FORMAT.format(tpCost) + " монет"
                }, Material.ENDER_PEARL, hasClanTp, tpCost, canUpgradeFeatures, clan));

        // Strength 1
        boolean hasStr1 = clan.hasFeature("buff-strength-1");
        inv.setItem(20, buildBuffItem(clan, "buff-strength-1", "💪 Бафф Сила I",
                new String[]{"&7Все участники получают Силу I", "&7на 15 минут"},
                Material.BLAZE_POWDER, hasStr1, 30000, canUpgradeFeatures));

        // Strength 2
        boolean hasStr2 = clan.hasFeature("buff-strength-2");
        inv.setItem(21, buildBuffItem(clan, "buff-strength-2", "💪 Бафф Сила II",
                new String[]{"&7Все участники получают Силу II", "&7на 15 минут"},
                Material.BLAZE_ROD, hasStr2, 60000, canUpgradeFeatures));

        // Speed 1
        boolean hasSpd1 = clan.hasFeature("buff-speed-1");
        inv.setItem(22, buildBuffItem(clan, "buff-speed-1", "👟 Бафф Скорость I",
                new String[]{"&7Все участники получают Скорость I", "&7на 15 минут"},
                Material.SUGAR, hasSpd1, 20000, canUpgradeFeatures));

        // Speed 2
        boolean hasSpd2 = clan.hasFeature("buff-speed-2");
        inv.setItem(23, buildBuffItem(clan, "buff-speed-2", "👟 Бафф Скорость II",
                new String[]{"&7Все участники получают Скорость II", "&7на 15 минут"},
                Material.RABBIT_FOOT, hasSpd2, 50000, canUpgradeFeatures));

        // Fire Resistance
        boolean hasFire = clan.hasFeature("buff-fire-resistance");
        inv.setItem(24, buildBuffItem(clan, "buff-fire-resistance", "🔥 Бафф Огнестойкость",
                new String[]{"&7Все участники получают Огнестойкость", "&7на 20 минут"},
                Material.MAGMA_CREAM, hasFire, 40000, canUpgradeFeatures));

        // Invisibility
        boolean hasInvis = clan.hasFeature("buff-invisibility");
        inv.setItem(25, buildBuffItem(clan, "buff-invisibility", "👻 Бафф Невидимость",
                new String[]{"&7Все участники получают Невидимость", "&7на 5 минут"},
                Material.FERMENTED_SPIDER_EYE, hasInvis, 50000, canUpgradeFeatures));

        // Back
        inv.setItem(45, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        // Activate buff buttons (if purchased)
        if (isOwner || canUpgradeFeatures) {
            inv.setItem(46, new ItemBuilder(Material.BEACON)
                    .name("&a&l▶ Активировать баффы")
                    .lore("&7Откройте меню активации баффов",
                            "",
                            "&eНажмите для открытия")
                    .build());
        }

        player.openInventory(inv);
    }

    private static org.bukkit.inventory.ItemStack buildShopUpgradeItem(Clan clan, int level, long cost,
                                                                       boolean purchased, boolean canBuy) {
        String status = purchased ? "&a&l✔ КУПЛЕНО" : (clan.getBalance() >= cost ? "&eДоступно" : "&cНедостаточно средств");
        Material mat = purchased ? Material.EMERALD : Material.GOLD_INGOT;
        ItemBuilder builder = new ItemBuilder(mat)
                .name((purchased ? "&a" : "&e") + "&lМагазин Ур. " + level)
                .lore(
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "&7Предметов в магазине: &f" + switch (level) { case 2 -> 2; case 3 -> 3; case 4 -> 5; default -> 1; },
                        "&7Стоимость: &6" + FORMAT.format(cost) + " монет",
                        "",
                        status,
                        !purchased && !canBuy ? "&cНет прав для покупки" : "",
                        !purchased ? "&eНажмите для покупки" : "",
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                );
        if (purchased) builder.glowing();
        return builder.build();
    }

    private static org.bukkit.inventory.ItemStack buildFeatureItem(Clan clan, String featureId, String name,
                                                                   String[] desc, Material mat, boolean purchased,
                                                                   long cost, boolean canBuy, Clan c) {
        String status = purchased ? "&a&l✔ КУПЛЕНО" : (clan.getBalance() >= cost ? "&eДоступно" : "&cНедостаточно средств");
        ItemBuilder builder = new ItemBuilder(purchased ? Material.EMERALD : mat)
                .name((purchased ? "&a" : "&b") + "&l" + name);

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        for (String d : desc) lore.add(d);
        lore.add("");
        lore.add(status);
        if (!purchased && !canBuy) lore.add("&cНет прав для покупки");
        if (!purchased) lore.add("&eНажмите для покупки");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        builder.lore(lore);
        if (purchased) builder.glowing();
        return builder.build();
    }

    private static org.bukkit.inventory.ItemStack buildBuffItem(Clan clan, String featureId, String name,
                                                                String[] desc, Material mat, boolean purchased,
                                                                long cost, boolean canBuy) {
        String status = purchased ? "&a&l✔ КУПЛЕНО" : (clan.getBalance() >= cost ? "&eДоступно" : "&cНедостаточно средств");
        ItemBuilder builder = new ItemBuilder(purchased ? Material.EMERALD : mat)
                .name((purchased ? "&a" : "&d") + "&l" + name);

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        for (String d : desc) lore.add(d);
        lore.add("&7Стоимость: &6" + FORMAT.format(cost) + " монет");
        lore.add("");
        lore.add(status);
        if (!purchased && !canBuy) lore.add("&cНет прав для покупки");
        if (!purchased) lore.add("&eНажмите для покупки");
        lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        builder.lore(lore);
        if (purchased) builder.glowing();
        return builder.build();
    }

    private static final DecimalFormat FORMAT_LOCAL = new DecimalFormat("#,###");

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).name("&0");
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) inv.setItem(i, glass.build());
        for (int i = size - 9; i < size; i++) inv.setItem(i, glass.build());
        for (int i = 0; i < rows; i++) {
            inv.setItem(i * 9, glass.build());
            inv.setItem(i * 9 + 8, glass.build());
        }
    }
}