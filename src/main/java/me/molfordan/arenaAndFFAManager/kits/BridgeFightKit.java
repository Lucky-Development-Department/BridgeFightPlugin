package me.molfordan.arenaAndFFAManager.kits;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Random;

public class BridgeFightKit {

    private final Random random = new Random();

    public void giveKit(Player player) {

        // Random team color
        boolean redTeam = random.nextBoolean();
        Color color = redTeam ? Color.RED : Color.LIME;
        short woolColorData = redTeam ? (short) 14 : (short) 5; // unused unless you add wool

        // Leather pieces (helmet + chestplate) - colored
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, color, Enchantment.PROTECTION_ENVIRONMENTAL, 3);
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, color, Enchantment.PROTECTION_ENVIRONMENTAL, 3);

        // Iron pieces
        ItemStack leggings = createItem(Material.IRON_LEGGINGS, Enchantment.PROTECTION_ENVIRONMENTAL, 3);
        ItemStack boots = createItem(Material.IRON_BOOTS, Enchantment.PROTECTION_ENVIRONMENTAL, 3);

        // Weapon
        ItemStack sword = createItem(Material.STONE_SWORD, Enchantment.DAMAGE_ALL, 1);

        // Apply
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        player.getInventory().addItem(sword);
    }

    /**
     * Creates colored leather armor in a robust way:
     * - safe null-checks
     * - clone meta to avoid shared instances on some forks
     * - apply color, then enchant, then unbreakable (matching working kit)
     */
    private ItemStack createColoredArmor(Material material, Color color, Enchantment enchant, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof LeatherArmorMeta)) return item; // safety

        LeatherArmorMeta meta = (LeatherArmorMeta) rawMeta.clone();

        // apply color first
        meta.setColor(color);

        // apply enchant AFTER color (this order matches your working BuildFFAKit)
        meta.addEnchant(enchant, level, true);

        // set unbreakable last
        meta.spigot().setUnbreakable(true);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, Enchantment enchant, int level) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchant, level, true);
            meta.spigot().setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, int amount, short data) {
        ItemStack item = new ItemStack(material, amount, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.spigot().setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // If you still use the hasDurability helper elsewhere, keep it in a utility class.
}
