package ru.clanplugin.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
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

    public ShopItem(String id, String displayName, Material material, int quantity, double priceEach) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.enchantment = null;
        this.enchantLevel = 0;
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
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getQuantity() { return quantity; }
    public double getPriceEach() { return priceEach; }
    public double getTotalPrice() { return priceEach * quantity; }
    public Enchantment getEnchantment() { return enchantment; }
    public int getEnchantLevel() { return enchantLevel; }

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

        if (enchantment != null) {
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
        }
        return stack;
    }
}
