package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanMembersMenu {

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        Inventory inv = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8👥 &bУчастники клана &8[" + clan.getMemberCount() + "]"));

        fillBorder(inv);

        int slot = 10;
        for (UUID memberUuid : clan.getMembers()) {
            if (slot > 44) break;
            if (slot % 9 == 8) slot += 2; // skip border

            OfflinePlayer member = Bukkit.getOfflinePlayer(memberUuid);
            String role;
            String roleColor;
            if (clan.isOwner(memberUuid)) {
                role = "Лидер";
                roleColor = "&6";
            } else if (clan.isOfficer(memberUuid)) {
                role = "Офицер";
                roleColor = "&b";
            } else {
                role = "Участник";
                roleColor = "&7";
            }

            boolean online = Bukkit.getPlayer(memberUuid) != null;
            List<String> lore = new ArrayList<>();
            lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("&7Роль: " + roleColor + role);
            lore.add("&7Статус: " + (online ? "&aОнлайн" : "&cОффлайн"));
            if (online) {
                Player onlinePlayer = Bukkit.getPlayer(memberUuid);
                if (onlinePlayer != null) {
                    double health = Math.round(onlinePlayer.getHealth() * 10.0) / 10.0;
                    int ping = onlinePlayer.getPing();
                    lore.add("&7Здоровье: &c" + health + " ❤");
                    lore.add("&7Пинг: &a" + ping + " ms");
                }
            }
            if (!clan.isOwner(memberUuid) && clan.isOwner(player.getUniqueId())) {
                lore.add("");
                lore.add("&eЛКМ — управление");
                lore.add("&cПКМ — исключить");
            }
            lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬");

            inv.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .name(roleColor + "&l" + (member.getName() != null ? member.getName() : "Неизвестно"))
                    .lore(lore)
                    .build());

            slot++;
        }

        // Back
        inv.setItem(45, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        player.openInventory(inv);
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).name("&0");
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
