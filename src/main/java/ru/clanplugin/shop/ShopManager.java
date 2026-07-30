package ru.clanplugin.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.clanplugin.ClanPlugin;

import java.util.*;

public class ShopManager {

    private final ClanPlugin plugin;
    private final List<ShopItem> allItems;
    private List<ShopItem> currentRotation;
    private long lastRotationTime;

    public ShopManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.allItems = new ArrayList<>();
        this.currentRotation = new ArrayList<>();
        initAllItems();
        rotateShop();
    }

    private void initAllItems() {
        // All possible shop items
        allItems.add(new ShopItem("dirt_500", "Земля", Material.DIRT, 1, 1, 500));
        allItems.add(new ShopItem("beacon", "Маяк", Material.BEACON, 1, 2000, 1));
        allItems.add(new ShopItem("netherite_ingot_5", "Незеритовый слиток", Material.NETHERITE_INGOT, 1, 700, 5));
        allItems.add(new ShopItem("cobblestone_500", "Булыжник", Material.COBBLESTONE, 1, 1, 500));
        allItems.add(new ShopItem("netherite_upgrade_3", "Незеритовое улучшение", Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 1200, 3));
        allItems.add(new ShopItem("mending_5", "Зачарование Починка", Material.ENCHANTED_BOOK, 1, 2000, Enchantment.MENDING, 1, 5));
        allItems.add(new ShopItem("prot3_3", "Зачарование Защита III", Material.ENCHANTED_BOOK, 1, 400, Enchantment.PROTECTION, 3, 3));
        allItems.add(new ShopItem("prot4_3", "Зачарование Защита IV", Material.ENCHANTED_BOOK, 1, 1700, Enchantment.PROTECTION, 4, 3));
        allItems.add(new ShopItem("unbreaking3_3", "Зачарование Прочность III", Material.ENCHANTED_BOOK, 1, 1000, Enchantment.UNBREAKING, 3, 3));
        allItems.add(new ShopItem("efficiency4_3", "Зачарование Эффективность IV", Material.ENCHANTED_BOOK, 1, 500, Enchantment.EFFICIENCY, 4, 3));
        allItems.add(new ShopItem("efficiency5_2", "Зачарование Эффективность V", Material.ENCHANTED_BOOK, 1, 1300, Enchantment.EFFICIENCY, 5, 2));
        allItems.add(new ShopItem("grass_500", "Дёрн", Material.GRASS_BLOCK, 500, 1, 500));
        allItems.add(new ShopItem("diamond_pickaxe_3", "Алмазная кирка", Material.DIAMOND_PICKAXE, 1, 400, 3));
        allItems.add(new ShopItem("netherite_pickaxe", "Незеритовая кирка", Material.NETHERITE_PICKAXE, 1, 3000, 1));
        allItems.add(new ShopItem("ender_pearl_64", "Эндер жемчуг", Material.ENDER_PEARL, 1, 8, 64));
        allItems.add(new ShopItem("fire_resistance_7", "Зелье Огнестойкости", Material.POTION, 1, 400, PotionType.FIRE_RESISTANCE, 7));
        allItems.add(new ShopItem("strength1_5", "Зелье Силы I", Material.POTION, 1, 700, PotionType.STRENGTH, 5));
        allItems.add(new ShopItem("strength2_3", "Зелье Силы II", Material.POTION, 1, 1300, PotionType.STRONG_STRENGTH, 3));
        allItems.add(new ShopItem("speed1_1", "Зелье Скорости I", Material.POTION, 1, 400, PotionType.SWIFTNESS, 1));
        allItems.add(new ShopItem("diamond_64", "Алмазы", Material.DIAMOND, 1, 25, 64));
        allItems.add(new ShopItem("lapis_128", "Лазурит", Material.LAPIS_LAZULI, 1, 3, 128));
        allItems.add(new ShopItem("redstone_256", "Редстоун", Material.REDSTONE, 1, 4, 256));
        allItems.add(new ShopItem("stone_256", "Камень", Material.STONE, 1, 2, 256));
    }

    public void rotateShop() {
        Collections.shuffle(allItems);
        // Max 5 items (for level 4), we'll pick based on level when showing
        currentRotation = new ArrayList<>(allItems.subList(0, Math.min(5, allItems.size())));
        lastRotationTime = System.currentTimeMillis();
    }

    public List<ShopItem> getItemsForLevel(int level) {
        int count = switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            default -> 1;
        };
        return currentRotation.subList(0, Math.min(count, currentRotation.size()));
    }

    public ShopItem getItemById(String id) {
        return currentRotation.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst().orElse(null);
    }

    public long getTimeUntilNextRotation() {
        long updateInterval = plugin.getConfig().getLong("shop.update-interval", 86400) * 1000;
        return Math.max(0, (lastRotationTime + updateInterval) - System.currentTimeMillis());
    }

    public String getTimeUntilNextRotationFormatted() {
        long ms = getTimeUntilNextRotation();
        long hours = ms / (1000 * 60 * 60);
        long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (ms % (1000 * 60)) / 1000;
        return hours + "ч " + minutes + "м " + seconds + "с";
    }

    public void startRotationTask() {
        long updateInterval = plugin.getConfig().getLong("shop.update-interval", 86400) * 20; // ticks
        new BukkitRunnable() {
            @Override
            public void run() {
                rotateShop();
                plugin.getLogger().info("Shop rotated! New items available.");
                // Notify online players
                plugin.getServer().broadcast(
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                .legacyAmpersand().deserialize(
                                        plugin.getPrefix() + "&aМагазин клана обновлён! &7Новые товары доступны в &e/clan shop")
                );
            }
        }.runTaskTimer(plugin, updateInterval, updateInterval);
    }
}