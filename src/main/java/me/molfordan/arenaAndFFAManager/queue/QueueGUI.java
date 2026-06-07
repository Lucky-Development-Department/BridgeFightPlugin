package me.molfordan.arenaAndFFAManager.queue;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class QueueGUI {

    private final ArenaAndFFAManager plugin;

    public QueueGUI(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.BLUE + "Select Queue Type");
        
        ItemStack solo = new ItemStack(Material.IRON_SWORD);
        ItemMeta soloMeta = solo.getItemMeta();
        soloMeta.setDisplayName(ChatColor.GOLD + "SOLO Queue");
        soloMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to join a solo match."));
        solo.setItemMeta(soloMeta);
        
        inv.setItem(3, solo);
        // Placeholder for DUO/PARTY
        
        player.openInventory(inv);
    }

    public void openSoloType(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.BLUE + "Select Solo Mode");
        
        ItemStack unranked = new ItemStack(Material.IRON_INGOT);
        ItemMeta unrankedMeta = unranked.getItemMeta();
        unrankedMeta.setDisplayName(ChatColor.GREEN + "UNRANKED");
        unrankedMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Join an unranked solo match."));
        unranked.setItemMeta(unrankedMeta);
        
        inv.setItem(3, unranked);
        
        player.openInventory(inv);
    }
}
