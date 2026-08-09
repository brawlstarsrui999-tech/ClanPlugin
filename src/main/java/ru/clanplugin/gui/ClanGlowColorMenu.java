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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Меню выбора цвета свечения для конкретного участника клана.
 * Открывается из {@link ClanGlowMenu}. Только для лидера клана.
 */
public class ClanGlowColorMenu {

    /** viewer uuid -> member uuid, чей цвет сейчас редактируется. */
    private static final Map<UUID, UUID> TARGETS = new HashMap<>();

    public static UUID getTarget(UUID viewer) {
        return TARGETS.get(viewer);
    }

    public static void clearSession(UUID viewer) {
        TARGETS.remove(viewer);
    }

    public static void open(Player player, ClanPlugin plugin, UUID targetUuid) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null || !clan.isOwner(player.getUniqueId())) return;
        if (!clan.isMember(targetUuid)) {
            ClanGlowMenu.open(player, plugin);
            return;
        }
        TARGETS.put(player.getUniqueId(), targetUuid);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : "Игрок";

        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer
                .legacyAmpersand().deserialize("&8🎨 Цвет свечения: &e" + targetName));

        fillBorder(inv);

        // Голова цели с текущим состоянием
        String currentColorName = clan.getGlowColor(targetUuid);
        boolean glowOn = clan.isGlowEnabled(targetUuid);
        ChatColor currentColor = glowOn ? clan.getGlowChatColor(targetUuid) : ChatColor.GRAY;
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&" + currentColor.getChar() + "&l" + targetName)
                .lore(
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                        "&7Текущее свечение: " + (glowOn
                                ? "&" + currentColor.getChar() + "██ &f" + GlowColors.russian(currentColorName)
                                : "&8██ &7выключено"),
                        "",
                        "&7Свечение видят только",
                        "&7участники вашего клана.",
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
                ).build());

        // 16 доступных цветов
        for (int i = 0; i < GlowColors.COLORS.size() && i < GlowColors.COLOR_SLOTS.length; i++) {
            GlowColors.GlowColor color = GlowColors.COLORS.get(i);
            int slot = GlowColors.COLOR_SLOTS[i];
            boolean current = glowOn && currentColorName.equalsIgnoreCase(color.name());

            ItemBuilder builder = new ItemBuilder(color.icon())
                    .name("&" + color.chatColor().getChar() + "&l" + color.russian())
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&" + color.chatColor().getChar() + "█████ Предпросмотр",
                            "",
                            current ? "&a✔ Текущий цвет" : "&eНажмите, чтобы выбрать",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬"
                    );
            if (current) builder.glowing();
            inv.setItem(slot, builder.build());
        }

        // Выключить свечение
        inv.setItem(GlowColors.OFF_SLOT, new ItemBuilder(Material.BARRIER)
                .name("&c&lВыключить свечение")
                .lore(
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬",
                        "&7Участник не будет светиться",
                        "&7для соклановцев.",
                        "",
                        glowOn ? "&eНажмите, чтобы выключить" : "&a✔ Сейчас выключено",
                        "&8▬▬▬▬▬▬▬▬▬▬▬▬"
                ).build());

        // Назад к списку участников
        inv.setItem(GlowColors.BACK_SLOT, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7К списку участников")
                .build());

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
    }
}