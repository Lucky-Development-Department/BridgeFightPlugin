package me.molfordan.arenaAndFFAManager.commands;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;

public class GUIStatsCommand implements CommandExecutor {

    private final ArenaAndFFAManager plugin;

    public GUIStatsCommand(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use GUI commands.");
            return true;
        }

        Player viewer = (Player) sender;

        // /stats → show own stats
        if (args.length == 0) {
            openGUIAsync(viewer, viewer.getUniqueId(), viewer.getName());
            return true;
        }

        String targetName = args[0];

        // Try online first
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            openGUIAsync(viewer, online.getUniqueId(), online.getName());
            return true;
        }

        // Offline player
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        if (offline == null || (!offline.hasPlayedBefore() && !offline.isOnline())) {
            viewer.sendMessage("§cPlayer not found.");
            return true;
        }

        openGUIAsync(viewer, offline.getUniqueId(), offline.getName());
        return true;
    }


    // ---------------------------------------------------------------------
    // ASYNC LOAD → SYNC GUI OPEN
    // ---------------------------------------------------------------------
    private void openGUIAsync(Player viewer, UUID uuid, String username) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {

                PlayerStats stats = plugin.getStatsManager().loadPlayer(uuid, username);
                if (stats == null) {
                    viewer.sendMessage("§cFailed to load stats.");
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        openGUI(viewer, stats);
                    }
                });
            }
        });
    }


    // ---------------------------------------------------------------------
    // BUILD THE GUI
    // ---------------------------------------------------------------------
    private void openGUI(Player viewer, PlayerStats stats) {

        Inventory inv = Bukkit.createInventory(null, 27, "§6Stats: §e" + stats.getUsername());

        // Border glass (Java 8-compatible legacy)
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

        // Player head (Java-8 compatible)
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwner(stats.getUsername());
        sm.setDisplayName("§e§l" + stats.getUsername());
        sm.setLore(Arrays.asList(
                "§7UUID:",
                "§f" + stats.getUuid().toString()
        ));
        head.setItemMeta(sm);
        inv.setItem(13, head);

        // BridgeFight stats
        ItemStack bridge = new ItemStack(Material.STONE_SWORD);
        ItemMeta bm = bridge.getItemMeta();
        bm.setDisplayName("§c§lBridge Fight Stats");
        bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        bm.setLore(Arrays.asList(
                "§7Kills: §a" + stats.getBridgeKills(),
                "§7Deaths: §a" + stats.getBridgeDeaths(),
                "§7Current Streak: §a" + stats.getBridgeStreak(),
                "§7Highest Streak: §6" + stats.getBridgeHighestStreak()
        ));
        bridge.setItemMeta(bm);
        inv.setItem(11, bridge);

        // BuildFFA stats
        ItemStack build = new ItemStack(Material.WOOL, 1, (short) 14); // red wool
        ItemMeta wm = build.getItemMeta();
        wm.setDisplayName("§e§lBuild FFA Stats");
        wm.setLore(Arrays.asList(
                "§7Kills: §a" + stats.getBuildKills(),
                "§7Deaths: §a" + stats.getBuildDeaths(),
                "§7Current Streak: §a" + stats.getBuildStreak(),
                "§7Highest Streak: §6" + stats.getBuildHighestStreak()
        ));
        build.setItemMeta(wm);
        inv.setItem(15, build);

        viewer.openInventory(inv);
    }
}
