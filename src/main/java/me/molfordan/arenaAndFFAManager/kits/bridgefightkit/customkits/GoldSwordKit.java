package me.molfordan.arenaAndFFAManager.kits.bridgefightkit.customkits;

import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.BaseKit;
import me.molfordan.arenaAndFFAManager.kits.bridgefightkit.KitItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public class GoldSwordKit extends BaseKit {

    public GoldSwordKit() {
        requiredKills = 10;

        items.add(new KitItem(Material.GOLD_SWORD)
                .enchant(Enchantment.DAMAGE_ALL, 1));
    }

    @Override
    public String getName() {
        return "Gold Sword Kit";
    }

    @Override
    public void apply(Player player) {
        give(player);
    }
}
