package me.molfordan.arenaAndFFAManager.gui;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class StatsGUI {

    private final ArenaAndFFAManager plugin;

    public StatsGUI(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.BLUE + "Your Statistics");
        UUID uuid = player.getUniqueId();
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);

        // Ranked
        inv.setItem(11, createItem(Material.DIAMOND_SWORD, ChatColor.GOLD + "Ranked Stats", Arrays.asList(
            ChatColor.GRAY + "ELO: " + ChatColor.YELLOW + stats.getRankedElo(),
            ChatColor.GRAY + "Peak ELO: " + ChatColor.YELLOW + stats.getPeakElo(),
            ChatColor.GRAY + "Wins: " + ChatColor.GREEN + stats.getRankedWins(),
            ChatColor.GRAY + "Losses: " + ChatColor.RED + stats.getRankedLosses(),
            ChatColor.GRAY + "Kills: " + ChatColor.WHITE + stats.getRankedKills(),
            ChatColor.GRAY + "Deaths: " + ChatColor.WHITE + stats.getRankedDeaths(),
            ChatColor.GRAY + "Beds: " + ChatColor.AQUA + stats.getRankedBeds()
        )));

        // Unranked
        inv.setItem(13, createItem(Material.IRON_SWORD, ChatColor.GRAY + "Unranked Stats", Arrays.asList(
            ChatColor.GRAY + "Wins: " + ChatColor.GREEN + stats.getUnrankedWins(),
            ChatColor.GRAY + "Losses: " + ChatColor.RED + stats.getUnrankedLosses(),
            ChatColor.GRAY + "Kills: " + ChatColor.WHITE + stats.getUnrankedKills(),
            ChatColor.GRAY + "Deaths: " + ChatColor.WHITE + stats.getUnrankedDeaths(),
            ChatColor.GRAY + "Beds: " + ChatColor.AQUA + stats.getUnrankedBeds(),
            ChatColor.GRAY + "Best Streak: " + ChatColor.YELLOW + stats.getBestUnrankedStreak()
        )));

        // Legacy (Bridge/Build)
        inv.setItem(15, createItem(Material.WOOL, ChatColor.LIGHT_PURPLE + "Legacy Stats", Arrays.asList(
            ChatColor.GRAY + "Bridge Kills: " + ChatColor.WHITE + stats.getBridgeKills(),
            ChatColor.GRAY + "Bridge Deaths: " + ChatColor.WHITE + stats.getBridgeDeaths(),
            ChatColor.GRAY + "Build Kills: " + ChatColor.WHITE + stats.getBuildKills(),
            ChatColor.GRAY + "Build Deaths: " + ChatColor.WHITE + stats.getBuildDeaths()
        )));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, java.util.List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
