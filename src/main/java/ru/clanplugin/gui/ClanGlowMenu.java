package ru.clanplugin.gui;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.glow.GlowColors;
import ru.clanplugin.utils.ItemBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Меню "✨ Свечение участников" — список участников клана с их текущим
 * цветом свечения. Доступно только лидеру клана: отсюда он задаёт
 * каждому участнику индивидуальный цвет свечения.
 *
 * Свечение видят только участники клана — посторонние его не видят.
 */
public class ClanGlowMenu {

    /** viewer uuid -> (slot -> member uuid) для текущей открытой копии меню. */
    private static final Map<UUID, Map<Integer, UUID>> SELECTIONS = new HashMap<>();

    public static Map<Integer, UUID> getSelection(UUID viewer) {
        return SELECTIONS.get(viewer);
    }

    public static void clearSession(UUID viewer) {
        SELECTIONS.remove(viewer);
        ClanGlowColorMenu.clearSession(viewer);
    }

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null || !clan.isOwner(player.getUniqueId())) return;

        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer
                .legacyAmpersand().deserialize("&8✨ &dСвечение участников"));

        fillBorder(inv);

        // Инфо-панель
        inv.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&d&l✨ Приватное свечение клана")
                .lore(
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "&7Свечение видят &aтолько участники клана&7,",
                        "&7посторонние игроки его &cне видят&7.",
                        "",
                        "&7У каждого участника может быть",
                        "&7свой собственный цвет свечения.",
                        "",
                        "&eКликните на участника,",
                        "&eчтобы выбрать ему цвет.",
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                ).build());

        Map<Integer, UUID> slotMap = new HashMap<>();
        SELECTIONS.put(player.getUniqueId(), slotMap);

        int slot = 10;
        for (UUID memberUuid : clan.getMembers()) {
            if (slot > 43) break;
            if (slot % 9 == 8) slot += 2; // пропуск рамки

            OfflinePlayer member = Bukkit.getOfflinePlayer(memberUuid);
            String memberName = member.getName() != null ? member.getName() : "Неизвестно";

            String role;
            if (clan.isOwner(memberUuid)) role = "&6Лидер";
            else if (clan.isOfficer(memberUuid)) role = "&bОфицер";
            else role = "&7Участник";

            String colorName = clan.getGlowColor(memberUuid);
            boolean glowOn = clan.isGlowEnabled(memberUuid);
            ChatColor color = glowOn ? clan.getGlowChatColor(memberUuid) : ChatColor.GRAY;

            List<String> lore = new ArrayList<>();
            lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            lore.add("&7Роль: " + role);
            lore.add("&7Статус: " + (member.isOnline() ? "&aОнлайн" : "&cОффлайн"));
            lore.add("&7Свечение: " + (glowOn
                    ? "&" + color.getChar() + "██ &f" + GlowColors.russian(colorName)
                    : "&8██ &7выключено"));
            lore.add("");
            lore.add("&eЛКМ — изменить цвет");
            lore.add("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

            inv.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&" + color.getChar() + "&l" + memberName)
                    .lore(lore)
                    .build());
            slotMap.put(slot, memberUuid);
            slot++;
        }

        // Назад
        inv.setItem(45, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться в меню клана")
                .build());

        player.openInventory(inv);
    }

    private static void fillBorder(Inventory inv) {
        ItemBuilder glass = new ItemBuilder(Material.PINK_STAINED_GLASS_PANE).name("&0");
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