package me.molfordan.arenaAndFFAManager.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.placeholder.LeaderboardPlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUILeaderboardMain {

    private final ArenaAndFFAManager plugin;

    public GUILeaderboardMain(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Leaderboard Menu");

        // BridgeFight icon
        ItemStack bridge = new ItemStack(Material.STONE_SWORD);
        ItemMeta bm = bridge.getItemMeta();
        bm.setDisplayName(ChatColor.BLUE + "BridgeFight Leaderboards");
        bridge.setItemMeta(bm);

        // BuildFFA icon
        ItemStack build = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta wm = build.getItemMeta();
        wm.setDisplayName(ChatColor.RED + "BuildFFA Leaderboards");
        build.setItemMeta(wm);

        inv.setItem(11, bridge);
        inv.setItem(15, build);

        p.openInventory(inv);
    }
}
