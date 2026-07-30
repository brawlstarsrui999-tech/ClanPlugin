package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

import java.text.DecimalFormat;

public class ClanMainMenu {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize("&8⚔ &6Клан: &e" + (clan != null ? clan.getName() : "—") + " &8⚔"));

        // Fill borders with gray glass
        fillBorder(inv);

        if (clan == null) {
            // Not in a clan
            inv.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("&cВы не состоите в клане")
                    .lore("&7Используйте &e/clan create &7для создания",
                            "&7или попросите приглашение")
                    .build());
        } else {
            boolean isOwner = clan.isOwner(player.getUniqueId());
            String ownerName = Bukkit.getOfflinePlayer(clan.getOwner()).getName();

            // Info panel - slot 13
            inv.setItem(13, new ItemBuilder(Material.GOLDEN_HELMET)
                    .name("&6&l⚔ Информация о клане")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Название: &e" + clan.getName(),
                            "&7Владелец: &a" + (ownerName != null ? ownerName : "Неизвестно"),
                            "&7Участников: &b" + clan.getMemberCount(),
                            "&7Баланс: &6" + FORMAT.format((int) clan.getBalance()) + " монет",
                            "&7Существует: &d" + clan.getCreatedAtFormatted(),
                            "&7Уровень магазина: &e" + clan.getShopLevel(),
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).glowing().build());

            // Members list - slot 11
            inv.setItem(11, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&b&l👥 Участники клана")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Всего: &f" + clan.getMemberCount(),
                            "&7Онлайн: &a" + clan.getOnlineMembers().size(),
                            "",
                            "&eНажмите для просмотра",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // Money info - slot 15
            inv.setItem(15, new ItemBuilder(Material.GOLD_INGOT)
                    .name("&6&l💰 Казна клана")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Баланс: &6" + FORMAT.format((int) clan.getBalance()) + " монет",
                            "",
                            "&aПополнить: &e/clan invest <сумма>",
                            "&cСнять: &e/clan withdraw <сумма>",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // Shop - slot 29
            inv.setItem(29, new ItemBuilder(Material.CHEST)
                    .name("&e&l🛒 Магазин клана")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Уровень: &a" + clan.getShopLevel(),
                            "&7Товаров: &f" + getItemCount(clan.getShopLevel()),
                            "",
                            "&eНажмите для открытия",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // Upgrades - slot 31 (owner only or with perm)
            boolean canUpgrade = isOwner ||
                    clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_SHOP) ||
                    clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_FEATURES);
            inv.setItem(31, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                    .name("&a&l⬆ Улучшения клана")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            canUpgrade ? "&eНажмите для просмотра" : "&cНедостаточно прав",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // PvP toggle - slot 33
            boolean pvpOn = clan.isPvpEnabled();
            inv.setItem(33, new ItemBuilder(pvpOn ? Material.RED_DYE : Material.GREEN_DYE)
                    .name(pvpOn ? "&c&l⚔ Дружественный огонь: ВКЛ" : "&a&l⚔ Дружественный огонь: ВЫКЛ")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            pvpOn ? "&cУрон по тиммейтам &lВКЛЮЧЁН" : "&aУрон по тиммейтам &lОТКЛЮЧЁН",
                            "",
                            (isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_PVP))
                                    ? "&eНажмите для переключения" : "&cНет прав",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // Homes - slot 20
            inv.setItem(20, new ItemBuilder(Material.RED_BED)
                    .name("&d&l🏠 Базы клана")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Баз: &f" + clan.getHomes().size(),
                            "",
                            "&eНажмите для просмотра",
                            isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_SETHOME)
                                    ? "&7/clan sethome <название>" : "",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // Top clans - slot 24
            inv.setItem(24, new ItemBuilder(Material.DIAMOND)
                    .name("&b&l🏆 Топ кланов")
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&eНажмите для просмотра топа",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());

            // Permissions (owner only) - slot 22
            if (isOwner) {
                inv.setItem(22, new ItemBuilder(Material.COMMAND_BLOCK)
                        .name("&c&l⚙ Права участников")
                        .lore(
                                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                                "&7Управление правами участников",
                                "",
                                "&eНажмите для открытия",
                                "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                        ).build());
            }

            // Leave/disband - slot 49
            if (isOwner) {
                inv.setItem(49, new ItemBuilder(Material.TNT)
                        .name("&4&l💣 Расформировать клан")
                        .lore("&cЭто удалит клан безвозвратно!",
                                "",
                                "&eДважды нажмите для подтверждения")
                        .build());
            } else {
                inv.setItem(49, new ItemBuilder(Material.OAK_DOOR)
                        .name("&c&l🚪 Покинуть клан")
                        .lore("&7Вы покинете клан &e" + clan.getName(),
                                "",
                                "&eНажмите для выхода")
                        .build());
            }
        }

        player.openInventory(inv);
    }

    private static String getItemCount(int level) {
        return switch (level) {
            case 1 -> "1";
            case 2 -> "2";
            case 3 -> "3";
            case 4 -> "5";
            default -> "1";
        };
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&0");
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
