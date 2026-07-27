package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.shop.ShopItem;
import ru.clanplugin.utils.ItemBuilder;

import java.text.DecimalFormat;
import java.util.List;

public class ClanShopMenu {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        Inventory inv = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8🛒 &6Магазин клана &8[Ур. " + clan.getShopLevel() + "]"));

        fillBorder(inv);

        // Info panel
        inv.setItem(4, new ItemBuilder(Material.CLOCK)
                .name("&e&l⏰ Обновление магазина")
                .lore(
                        "&7Следующее обновление: &a" + plugin.getShopManager().getTimeUntilNextRotationFormatted(),
                        "",
                        "&7Уровень магазина: &6" + clan.getShopLevel() + " &7/ 4",
                        "&7Баланс клана: &6" + FORMAT.format((int) clan.getBalance()) + " монет"
                ).build());

        // Shop items
        List<ShopItem> items = plugin.getShopManager().getItemsForLevel(clan.getShopLevel());
        int[] slots = {19, 21, 23, 25, 22}; // positions for items

        for (int i = 0; i < items.size(); i++) {
            ShopItem shopItem = items.get(i);
            inv.setItem(slots[i], shopItem.createDisplayItem());
        }

        // Locked slots for higher level
        int[] lockedSlots = {20, 22, 24};
        int maxItems = switch (clan.getShopLevel()) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            default -> 1;
        };

        if (maxItems < 5) {
            inv.setItem(slots[maxItems], new ItemBuilder(Material.IRON_BARS)
                    .name("&c&l🔒 Заблокировано")
                    .lore("&7Улучшите магазин клана",
                            "&7до следующего уровня")
                    .build());
        }

        // Back button
        inv.setItem(45, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        // Upgrade button
        boolean isOwner = clan.isOwner(player.getUniqueId());
        boolean canUpgrade = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_SHOP);
        if (canUpgrade && clan.getShopLevel() < 4) {
            int nextLevel = clan.getShopLevel() + 1;
            long cost = plugin.getConfig().getLong("upgrades.shop.level-" + nextLevel, 999999);
            inv.setItem(53, new ItemBuilder(Material.NETHER_STAR)
                    .name("&a&l⬆ Улучшить магазин")
                    .lore(
                            "&7Уровень: &e" + clan.getShopLevel() + " &7→ &a" + nextLevel,
                            "&7Стоимость: &6" + FORMAT.format(cost) + " монет",
                            "&7Казна: &6" + FORMAT.format((int) clan.getBalance()) + " монет",
                            "",
                            clan.getBalance() >= cost ? "&eНажмите для улучшения!" : "&cНедостаточно средств в казне"
                    ).glowing().build());
        } else if (clan.getShopLevel() >= 4) {
            inv.setItem(53, new ItemBuilder(Material.EMERALD)
                    .name("&a&lМаксимальный уровень!")
                    .lore("&7Ваш магазин максимально прокачан!")
                    .glowing().build());
        }

        player.openInventory(inv);
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&0");
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) inv.setItem(i, glass.build());
        for (int i = size - 9; i < size; i++) inv.setItem(i, glass.build());
        for (int i = 0; i < rows; i++) {
            inv.setItem(i * 9, glass.build());
            inv.setItem(i * 9 + 8, glass.build());
        }
        // Extra decoration
        ItemBuilder goldGlass = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE).name("&0");
        inv.setItem(0, goldGlass.build());
        inv.setItem(8, goldGlass.build());
        inv.setItem(45, goldGlass.build());
        inv.setItem(53, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("&0").build());
    }
}
