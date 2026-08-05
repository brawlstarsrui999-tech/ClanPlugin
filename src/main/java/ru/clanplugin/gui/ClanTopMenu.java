package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

import java.text.DecimalFormat;
import java.util.List;

public class ClanTopMenu {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");
    private static final Material[] MEDAL_MATS = {
            Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
            Material.STONE, Material.STONE, Material.STONE, Material.STONE,
            Material.STONE, Material.STONE, Material.STONE
    };
    private static final String[] MEDAL_COLORS = {
            "&6", "&7", "&c", "&f", "&f", "&f", "&f", "&f", "&f", "&f"
    };
    private static final String[] MEDALS = {"🥇", "🥈", "🥉", "4", "5", "6", "7", "8", "9", "10"};

    public static void open(Player player, ClanPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8🏆 &6Топ кланов по богатству"));

        fillBorder(inv);

        List<Clan> topClans = plugin.getClanManager().getTopClansByMoney(10);

        inv.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&6&l🏆 Топ кланов")
                .lore("&7Рейтинг кланов по сумме денег в казне",
                        "&7Всего кланов: &f" + plugin.getClanManager().getAllClans().size())
                .glowing().build());

        int[] slots = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};

        for (int i = 0; i < topClans.size(); i++) {
            Clan clan = topClans.get(i);
            String ownerName = Bukkit.getOfflinePlayer(clan.getOwner()).getName();
            String color = MEDAL_COLORS[i];
            Material mat = MEDAL_MATS[i];

            Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            boolean isPlayersClan = playerClan != null && playerClan.getId().equals(clan.getId());

            inv.setItem(slots[i], new ItemBuilder(mat)
                    .name(color + "&l" + MEDALS[i] + " " + clan.getName() + (isPlayersClan ? " &7(ваш)" : ""))
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Место: " + color + "#" + (i + 1),
                            "&7Казна: &6" + FORMAT.format((int) clan.getBalance()) + " монет",
                            "&7Участников: &b" + clan.getMemberCount(),
                            "&7Лидер: &a" + (ownerName != null ? ownerName : "Неизвестно"),
                            "&7Уровень магазина: &e" + clan.getShopLevel(),
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());
        }

        if (topClans.isEmpty()) {
            inv.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("&cКланов нет")
                    .lore("&7Создайте первый клан: &e/clan create")
                    .build());
        }

        // Highlight player's clan position
        Clan playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (playerClan != null) {
            int pos = topClans.indexOf(playerClan);
            inv.setItem(49, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&b&lВаш клан")
                    .lore(
                            "&7Клан: &e" + playerClan.getName(),
                            "&7Позиция в топе: &f" + (pos >= 0 ? "#" + (pos + 1) : ">" + topClans.size()),
                            "&7Казна: &6" + FORMAT.format((int) playerClan.getBalance()) + " монет"
                    ).build());
        }

        // Back
        inv.setItem(45, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        player.openInventory(inv);
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE).name("&0");
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
