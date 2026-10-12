package ru.clanplugin.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import ru.clanplugin.utils.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

public class ShopItem {

    private final String id;
    private final String displayName;
    private final Material material;
    private final int quantity;
    private final double priceEach;
    private final Enchantment enchantment;
    private final int enchantLevel;
    private final PotionType potionType;
    private final int dailyLimit; // 0 = unlimited

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = null;
        this.enchantLevel = 0;
        this.potionType = null;
        this.dailyLimit = 0;
    }

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach,
                    Enchantment enchantment, int enchantLevel) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = enchantment;
        this.enchantLevel = enchantLevel;
        this.potionType = null;
        this.dailyLimit = 0;
    }

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach, PotionType potionType) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = null;
        this.enchantLevel = 0;
        this.potionType = potionType;
        this.dailyLimit = 0;
    }

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach, int dailyLimit) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = null;
        this.enchantLevel = 0;
        this.potionType = null;
        this.dailyLimit = dailyLimit;
    }

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach,
                    Enchantment enchantment, int enchantLevel, int dailyLimit) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = enchantment;
        this.enchantLevel = enchantLevel;
        this.potionType = null;
        this.dailyLimit = dailyLimit;
    }

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach, PotionType potionType, int dailyLimit) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = null;
        this.enchantLevel = 0;
        this.potionType = potionType;
        this.dailyLimit = dailyLimit;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getQuantity() { return quantity; }
    public double getPriceEach() { return priceEach; }
    public double getTotalPrice() { return priceEach * quantity; }
    public Enchantment getEnchantment() { return enchantment; }
    public int getEnchantLevel() { return enchantLevel; }
    public PotionType getPotionType() { return potionType; }
    public int getDailyLimit() { return dailyLimit; }

    public ItemStack createDisplayItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Количество: &f" + quantity + " шт.");
        lore.add("&7Цена за 1: &6" + (int) priceEach + " монет");
        lore.add("&7Итого: &e" + (int) getTotalPrice() + " монет");
        lore.add("");
        lore.add("&aНажмите для покупки!");

        ItemBuilder builder = new ItemBuilder(material, quantity)
                .name("&6" + displayName)
                .lore(lore);

        if (enchantment != null || potionType != null) {
            builder.glowing();
        }

        return builder.build();
    }

    public ItemStack createGiveItem() {
        ItemStack stack = new ItemStack(material, quantity);
        if (enchantment != null) {
            if (material == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) stack.getItemMeta();
                meta.addStoredEnchant(enchantment, enchantLevel, true);
                stack.setItemMeta(meta);
            } else {
                stack.addUnsafeEnchantment(enchantment, enchantLevel);
            }
        } else if (potionType != null && material == Material.POTION) {
            if (stack.getItemMeta() instanceof PotionMeta meta) {
                meta.setBasePotionType(potionType);
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }
}