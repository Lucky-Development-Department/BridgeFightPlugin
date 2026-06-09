package me.molfordan.bridgefightplugin.kits.bridgefightkit.customkits;

import me.molfordan.bridgefightplugin.kits.bridgefightkit.BaseKit;
import me.molfordan.bridgefightplugin.kits.bridgefightkit.KitItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public class GoldArmorKit extends BaseKit {

    public GoldArmorKit() {
        requiredKills = 20;

        items.add(new KitItem(Material.GOLD_HELMET)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(39));

        items.add(new KitItem(Material.GOLD_CHESTPLATE)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(38));

        items.add(new KitItem(Material.GOLD_LEGGINGS)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(37));

        items.add(new KitItem(Material.GOLD_BOOTS)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(36));

        items.add(new KitItem(Material.GOLD_SWORD).enchant(Enchantment.DAMAGE_ALL, 1));
    }

    @Override
    public String getName() {
        return "Gold Armor Kit";
    }

    @Override
    public void apply(Player player) {
        give(player);
    }
}
