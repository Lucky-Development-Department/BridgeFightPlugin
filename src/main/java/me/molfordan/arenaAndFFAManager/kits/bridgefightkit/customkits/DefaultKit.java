package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits;

import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BaseKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.KitItem;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.Random;

public class DefaultKit extends BaseKit {

    public DefaultKit() {
        boolean red = new Random().nextBoolean();
        Color teamColor = red ? Color.RED : Color.LIME;

        requiredKills = 0;

        items.add(new KitItem(Material.LEATHER_HELMET)
                .colored(teamColor)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(39));

        items.add(new KitItem(Material.LEATHER_CHESTPLATE)
                .colored(teamColor)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(38));

        items.add(new KitItem(Material.IRON_LEGGINGS)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(37));

        items.add(new KitItem(Material.IRON_BOOTS)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3)
                .slot(36));

        items.add(new KitItem(Material.WOOD_SWORD)
                .enchant(Enchantment.DAMAGE_ALL, 1));
    }

    @Override
    public String getName() {
        return "Default Kit";
    }

    @Override
    public void apply(Player player) {
        give(player);
    }
}
