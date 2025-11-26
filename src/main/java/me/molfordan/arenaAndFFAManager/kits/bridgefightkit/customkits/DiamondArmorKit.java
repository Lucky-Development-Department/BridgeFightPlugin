package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits;

import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BaseKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.KitItem;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.Random;

public class DiamondArmorKit extends BaseKit {



    public DiamondArmorKit() {
        boolean red = new Random().nextBoolean();
        Color teamColor = red ? Color.RED : Color.LIME;

        requiredKills = 50;

        items.add(new KitItem(Material.LEATHER_HELMET)
                .colored(teamColor)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(39));

        items.add(new KitItem(Material.LEATHER_CHESTPLATE)
                .colored(teamColor)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(38));

        items.add(new KitItem(Material.DIAMOND_LEGGINGS)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(37));

        items.add(new KitItem(Material.DIAMOND_BOOTS)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(36));

        items.add(new KitItem(Material.DIAMOND_SWORD)
                .enchant(Enchantment.DAMAGE_ALL, 1));
    }

    @Override
    public String getName() {
        return "Diamond Kit";
    }

    @Override
    public void apply(Player player) {
        give(player);
    }
}
