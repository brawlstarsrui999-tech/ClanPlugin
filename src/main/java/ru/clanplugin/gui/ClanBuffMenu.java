package ru.clanplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.clanplugin.ClanPlugin;
import ru.clanplugin.clan.Clan;
import ru.clanplugin.utils.ItemBuilder;

import java.text.DecimalFormat;
import java.util.List;

public class ClanBuffMenu {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###");

    public static void open(Player player, ClanPlugin plugin) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        boolean isOwner = clan.isOwner(player.getUniqueId());
        boolean canUpgrade = isOwner || clan.hasOfficerPerm(player.getUniqueId(), Clan.PERM_UPGRADE_FEATURES);

        Inventory inv = Bukkit.createInventory(null, 36,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacyAmpersand().deserialize("&8✨ &dАктивация баффов клана"));

        fillBorder(inv);

        inv.setItem(4, new ItemBuilder(Material.BEACON)
                .name("&d&l✨ Баффы клана")
                .lore("&7Казна: &6" + FORMAT.format((int) clan.getBalance()) + " монет",
                        "&7Баффы применяются ко всем онлайн участникам",
                        "",
                        canUpgrade ? "&aВы можете активировать баффы" : "&cНет прав для активации")
                .glowing().build());

        // Strength 1
        addBuffButton(inv, 10, clan, "buff-strength-1", "💪 Сила I",
                "Все участники получают Силу I на 15 минут",
                Material.BLAZE_POWDER, 30000, canUpgrade);

        // Strength 2
        addBuffButton(inv, 11, clan, "buff-strength-2", "💪 Сила II",
                "Все участники получают Силу II на 15 минут",
                Material.BLAZE_ROD, 60000, canUpgrade);

        // Speed 1
        addBuffButton(inv, 12, clan, "buff-speed-1", "👟 Скорость I",
                "Все участники получают Скорость I на 15 минут",
                Material.SUGAR, 20000, canUpgrade);

        // Speed 2
        addBuffButton(inv, 13, clan, "buff-speed-2", "👟 Скорость II",
                "Все участники получают Скорость II на 15 минут",
                Material.RABBIT_FOOT, 50000, canUpgrade);

        // Fire resistance
        addBuffButton(inv, 14, clan, "buff-fire-resistance", "🔥 Огнестойкость",
                "Все участники получают Огнестойкость на 20 минут",
                Material.MAGMA_CREAM, 40000, canUpgrade);

        // Invisibility
        addBuffButton(inv, 20, clan, "buff-invisibility", "👻 Невидимость",
                "Все участники получают Невидимость на 5 минут",
                Material.FERMENTED_SPIDER_EYE, 50000, canUpgrade);

        // Back
        inv.setItem(27, new ItemBuilder(Material.ARROW)
                .name("&c&l← Назад")
                .lore("&7Вернуться к улучшениям")
                .build());

        player.openInventory(inv);
    }

    private static void addBuffButton(Inventory inv, int slot, Clan clan, String featureId,
                                      String name, String desc, Material mat, long cost, boolean canActivate) {
        boolean purchased = clan.hasFeature(featureId);
        boolean canAfford = clan.getBalance() >= cost;

        if (purchased) {
            inv.setItem(slot, new ItemBuilder(mat)
                    .name("&a&l✔ " + name)
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7" + desc,
                            "&7Разовая активация: &6" + FORMAT.format(cost / 10) + " монет",
                            "",
                            canActivate ? (clan.getBalance() >= cost / 10 ? "&eНажмите для активации!" : "&cНедостаточно средств") : "&cНет прав",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).glowing().build());
        } else {
            inv.setItem(slot, new ItemBuilder(Material.BARRIER)
                    .name("&c&l🔒 " + name)
                    .lore(
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬",
                            "&7" + desc,
                            "&cСначала купите это улучшение",
                            "&7в разделе улучшений",
                            "&8▬▬▬▬▬▬▬▬▬▬▬▬"
                    ).build());
        }
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

    // Apply buff to all online clan members
    public static void applyBuff(Clan clan, String featureId, ClanPlugin plugin) {
        List<Player> online = clan.getOnlineMembers();
        PotionEffectType type;
        int duration;
        int amplifier;

        switch (featureId) {
            case "buff-strength-1" -> { type = PotionEffectType.STRENGTH; duration = 15 * 60 * 20; amplifier = 0; }
            case "buff-strength-2" -> { type = PotionEffectType.STRENGTH; duration = 15 * 60 * 20; amplifier = 1; }
            case "buff-speed-1" -> { type = PotionEffectType.SPEED; duration = 15 * 60 * 20; amplifier = 0; }
            case "buff-speed-2" -> { type = PotionEffectType.SPEED; duration = 15 * 60 * 20; amplifier = 1; }
            case "buff-fire-resistance" -> { type = PotionEffectType.FIRE_RESISTANCE; duration = 20 * 60 * 20; amplifier = 0; }
            case "buff-invisibility" -> { type = PotionEffectType.INVISIBILITY; duration = 5 * 60 * 20; amplifier = 0; }
            default -> { return; }
        }

        PotionEffectType finalType = type;
        int finalDuration = duration;
        int finalAmplifier = amplifier;

        for (Player p : online) {
            p.addPotionEffect(new PotionEffect(finalType, finalDuration, finalAmplifier), true);
            p.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().deserialize(
                            plugin.getPrefix() + "&aВам применён клановый бафф! Удачи в бою!"));
        }
    }
}
