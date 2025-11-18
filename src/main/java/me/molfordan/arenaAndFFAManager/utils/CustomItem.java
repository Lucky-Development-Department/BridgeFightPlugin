package me.molfordan.arenaAndFFAManager.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class CustomItem {

    public static ItemStack getTeleportSnowball() {
        ItemStack snowball = new ItemStack(Material.SNOW_BALL);
        ItemMeta meta = snowball.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Teleport Snowball");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Hit a player to switch places"));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);// Fake enchant for glow
        snowball.setItemMeta(meta);
        return snowball;
    }


}
