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
        
        ItemStack solo = buildItem(Material.IRON_SWORD, ChatColor.GOLD + "SOLO Queue", ChatColor.GRAY + "1v1 Matchmaking");
        ItemStack duo = buildItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "DUO Queue", ChatColor.GRAY + "2v2 Matchmaking");
        ItemStack party = buildItem(Material.NAME_TAG, ChatColor.GOLD + "PARTY Queue", ChatColor.GRAY + "Party matches");
        
        inv.setItem(2, solo);
        inv.setItem(4, duo);
        inv.setItem(6, party);
        
        player.openInventory(inv);
    }

    public void openSoloType(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.BLUE + "Select Solo Mode");
        inv.setItem(3, buildItem(Material.IRON_INGOT, ChatColor.GREEN + "UNRANKED", ChatColor.GRAY + "Casual 1v1"));
        inv.setItem(5, buildItem(Material.GOLD_INGOT, ChatColor.YELLOW + "RANKED", ChatColor.GRAY + "Competitive 1v1"));
        player.openInventory(inv);
    }
    
    public void openDuoType(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.BLUE + "Select Duo Mode");
        inv.setItem(3, buildItem(Material.IRON_INGOT, ChatColor.GREEN + "UNRANKED", ChatColor.GRAY + "Casual 2v2"));
        inv.setItem(5, buildItem(Material.GOLD_INGOT, ChatColor.YELLOW + "RANKED", ChatColor.GRAY + "Competitive 2v2"));
        player.openInventory(inv);
    }

    public void openPartyType(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.BLUE + "Select Party Mode");
        inv.setItem(3, buildItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "PARTY FIGHT", ChatColor.GRAY + "Party vs Party"));
        inv.setItem(5, buildItem(Material.COMPASS, ChatColor.LIGHT_PURPLE + "PARTY SPLIT", ChatColor.GRAY + "Auto-balanced scrims"));
        player.openInventory(inv);
    }

    private ItemStack buildItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

}
