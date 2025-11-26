package me.molfordan.arenaAndFFAManager.kits.bridgefightkit;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.HashMap;
import java.util.Map;

public class KitItem {

    private final Material material;
    private final int amount;
    private final Map<Enchantment, Integer> enchants = new HashMap<>();
    private boolean unbreakable = true;
    private Color leatherColor = null;   // null = no color
    private Integer slot = null;         // null = addItem()

    public KitItem(Material material) {
        this(material, 1);
    }

    public KitItem(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public KitItem enchant(Enchantment ench, int level) {
        enchants.put(ench, level);
        return this;
    }

    public KitItem colored(Color color) {
        this.leatherColor = color;
        return this;
    }

    public KitItem slot(int slot) {
        this.slot = slot;
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            enchants.forEach((e, l) -> meta.addEnchant(e, l, true));
            meta.spigot().setUnbreakable(unbreakable);

            if (leatherColor != null && meta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) meta).setColor(leatherColor);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public Integer getSlot() {
        return slot;
    }
}
