package ru.clanplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder glowing() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder hideFlags() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
