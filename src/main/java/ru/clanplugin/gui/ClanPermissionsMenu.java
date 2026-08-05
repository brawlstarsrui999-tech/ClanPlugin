package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

public class ClanPermissionsMenu {

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null || !clan.isOwner(player.getUniqueId())) return;

        Inventory inv = Bukkit.createInventory(null, 27,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8⚙ &cПрава офицеров"));

        fillBorder(inv);

        // PVP permission
        boolean pvpPerm = clan.getOfficerPerms().getOrDefault(Clan.PERM_PVP, false);
        inv.setItem(10, new ItemBuilder(pvpPerm ? Material.GREEN_DYE : Material.RED_DYE)
                .name("&e⚔ Управление PvP")
                .lore("&7Офицеры могут включать/отключать",
                        "&7дружественный огонь клана",
                        "",
                        "Статус: " + (pvpPerm ? "&a&lВКЛЮЧЕНО" : "&c&lОТКЛЮЧЕНО"),
                        "",
                        "&eНажмите для переключения")
                .build());

        // Sethome permission
        boolean sethomePerm = clan.getOfficerPerms().getOrDefault(Clan.PERM_SETHOME, false);
        inv.setItem(11, new ItemBuilder(sethomePerm ? Material.GREEN_DYE : Material.RED_DYE)
                .name("&d🏠 Установка баз")
                .lore("&7Офицеры могут устанавливать",
                        "&7базы клана (/clan sethome)",
                        "",
                        "Статус: " + (sethomePerm ? "&a&lВКЛЮЧЕНО" : "&c&lОТКЛЮЧЕНО"),
                        "",
                        "&eНажмите для переключения")
                .build());

        // Upgrade shop permission
        boolean upgradeShopPerm = clan.getOfficerPerms().getOrDefault(Clan.PERM_UPGRADE_SHOP, false);
        inv.setItem(12, new ItemBuilder(upgradeShopPerm ? Material.GREEN_DYE : Material.RED_DYE)
                .name("&6🛒 Улучшение магазина")
                .lore("&7Офицеры могут прокачивать",
                        "&7магазин клана",
                        "",
                        "Статус: " + (upgradeShopPerm ? "&a&lВКЛЮЧЕНО" : "&c&lОТКЛЮЧЕНО"),
                        "",
                        "&eНажмите для переключения")
                .build());

        // Upgrade features permission
        boolean upgradeFeatPerm = clan.getOfficerPerms().getOrDefault(Clan.PERM_UPGRADE_FEATURES, false);
        inv.setItem(13, new ItemBuilder(upgradeFeatPerm ? Material.GREEN_DYE : Material.RED_DYE)
                .name("&a⬆ Улучшение способностей")
                .lore("&7Офицеры могут покупать",
                        "&7улучшения для клана",
                        "",
                        "Статус: " + (upgradeFeatPerm ? "&a&lВКЛЮЧЕНО" : "&c&lОТКЛЮЧЕНО"),
                        "",
                        "&eНажмите для переключения")
                .build());

        // Officer management
        inv.setItem(15, new ItemBuilder(Material.NAME_TAG)
                .name("&b&l👥 Управление офицерами")
                .lore("&7Используйте &e/clan officer <игрок>",
                        "&7для назначения/снятия офицера")
                .build());

        // Back
        inv.setItem(18, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        player.openInventory(inv);
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("&0");
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
