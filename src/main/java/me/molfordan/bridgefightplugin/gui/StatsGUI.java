package me.molfordan.bridgefightplugin.gui;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.object.PlayerStats;
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

    private final BridgeFightPlugin plugin;

    public StatsGUI(BridgeFightPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        openFor(player, player.getUniqueId(), player.getName());
    }

    public void openFor(Player viewer, UUID targetUuid, String targetName) {
        PlayerStats stats = plugin.getStatsManager().loadPlayer(targetUuid, targetName);
        if (stats == null) {
            viewer.sendMessage(ChatColor.RED + "Failed to load statistics.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Stats: " + ChatColor.YELLOW + stats.getUsername());

        // Border glass
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);

        int[] border = {
                0,1,2,3,4,5,6,7,8,
                9,17,
                18,19,20,21,22,23,24,25,26
        };

        for (int slot : border) {
            inv.setItem(slot, glass);
        }

        // Player head
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        sm.setOwner(stats.getUsername());
        sm.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + stats.getUsername());
        sm.setLore(Arrays.asList(
                ChatColor.GRAY + "UUID:",
                ChatColor.WHITE + stats.getUuid().toString()
        ));
        head.setItemMeta(sm);
        inv.setItem(13, head);

        // BridgeFight stats
        ItemStack bridge = new ItemStack(Material.STONE_SWORD);
        ItemMeta bm = bridge.getItemMeta();
        bm.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Bridge Fight Stats");
        bm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        bm.setLore(Arrays.asList(
                ChatColor.GRAY + "Kills: " + ChatColor.GREEN + stats.getBridgeKills(),
                ChatColor.GRAY + "Deaths: " + ChatColor.GREEN + stats.getBridgeDeaths(),
                ChatColor.GRAY + "Current Streak: " + ChatColor.GREEN + stats.getBridgeStreak(),
                ChatColor.GRAY + "Highest Streak: " + ChatColor.GOLD + stats.getBridgeHighestStreak()
        ));
        bridge.setItemMeta(bm);
        inv.setItem(11, bridge);

        // BuildFFA stats
        ItemStack build = new ItemStack(Material.WOOL, 1, (short) 14); // red wool
        ItemMeta wm = build.getItemMeta();
        wm.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Build FFA Stats");
        wm.setLore(Arrays.asList(
                ChatColor.GRAY + "Kills: " + ChatColor.GREEN + stats.getBuildKills(),
                ChatColor.GRAY + "Deaths: " + ChatColor.GREEN + stats.getBuildDeaths(),
                ChatColor.GRAY + "Current Streak: " + ChatColor.GREEN + stats.getBuildStreak(),
                ChatColor.GRAY + "Highest Streak: " + ChatColor.GOLD + stats.getBuildHighestStreak()
        ));
        build.setItemMeta(wm);
        inv.setItem(15, build);

        // BedFight/Ranked Stats (Bottom Row)
        ItemStack ranked = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta rm = ranked.getItemMeta();
        rm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Ranked BedFight Stats");
        rm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        rm.setLore(Arrays.asList(
                ChatColor.GRAY + "ELO: " + ChatColor.YELLOW + stats.getRankedElo(),
                ChatColor.GRAY + "Wins: " + ChatColor.GREEN + stats.getRankedWins(),
                ChatColor.GRAY + "Losses: " + ChatColor.RED + stats.getRankedLosses()
        ));
        ranked.setItemMeta(rm);
        inv.setItem(22, ranked);

        viewer.openInventory(inv);
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
