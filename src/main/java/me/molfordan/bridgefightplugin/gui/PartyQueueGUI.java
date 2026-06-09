package me.molfordan.bridgefightplugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PartyQueueGUI {

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Queue Selection");

        ItemStack partySplit = new ItemStack(Material.IRON_SWORD);
        ItemMeta splitMeta = partySplit.getItemMeta();
        splitMeta.setDisplayName(ChatColor.YELLOW + "Party Split");
        splitMeta.setLore(Arrays.asList(ChatColor.GRAY + "Split your party into 2 teams."));
        partySplit.setItemMeta(splitMeta);

        ItemStack partyMatch = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta matchMeta = partyMatch.getItemMeta();
        matchMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Party Match");
        matchMeta.setLore(Arrays.asList(ChatColor.GRAY + "Fight another party."));
        partyMatch.setItemMeta(matchMeta);

        ItemStack duoQueue = new ItemStack(Material.GOLD_SWORD);
        ItemMeta duoMeta = duoQueue.getItemMeta();
        duoMeta.setDisplayName(ChatColor.AQUA + "Duo Queue");
        duoMeta.setLore(Arrays.asList(ChatColor.GRAY + "Queue with another player."));
        duoQueue.setItemMeta(duoMeta);

        inv.setItem(2, partySplit);
        inv.setItem(4, partyMatch);
        inv.setItem(6, duoQueue);

        player.openInventory(inv);
    }
}
