package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

import java.util.Map;

public class ClanHomesMenu {

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        Inventory inv = Bukkit.createInventory(null, 36,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8🏠 &dБазы клана"));

        fillBorder(inv);

        // Info
        boolean isOwner = clan.isOwner(player.getUniqueId());
        boolean canSetHome = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_SETHOME);

        inv.setItem(4, new ItemBuilder(Material.MAP)
                .name("&d&lБазы клана &8[" + clan.getHomes().size() + "]")
                .lore(
                        "&7Нажмите на базу для телепортации",
                        canSetHome ? "&7Установить: &e/clan sethome <название>" : "",
                        "&7Убрать: &e/clan delhome <название>"
                ).build());

        int slot = 10;
        for (Map.Entry<String, Location> entry : clan.getHomes().entrySet()) {
            if (slot > 26) break;
            if (slot % 9 == 8) slot += 2;

            Location loc = entry.getValue();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "Неизвестно";

            inv.setItem(slot, new ItemBuilder(Material.RED_BED)
                    .name("&d&l🏠 " + entry.getKey())
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7Мир: &f" + worldName,
                            "&7X: &f" + (int) loc.getX(),
                            "&7Y: &f" + (int) loc.getY(),
                            "&7Z: &f" + (int) loc.getZ(),
                            "",
                            "&eНажмите для телепортации",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());
            slot++;
        }

        if (clan.getHomes().isEmpty()) {
            inv.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("&cБаз нет")
                    .lore("&7Используйте &e/clan sethome <название>",
                            "&7для установки базы")
                    .build());
        }

        // Back
        inv.setItem(27, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        player.openInventory(inv);
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.MAGENTA_STAINED_GLASS_PANE).name("&0");
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
