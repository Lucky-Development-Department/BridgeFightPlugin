package me.molfordan.arenaAndFFAManager.queue;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class QueueManager {
    private final ArenaAndFFAManager plugin;
    private final List<UUID> soloUnrankedQueue = new ArrayList<>();
    private final Random random = new Random();

    public QueueManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        startQueueTask();
    }

    public void addSoloUnranked(Player player) {
        if (!soloUnrankedQueue.contains(player.getUniqueId())) {
            soloUnrankedQueue.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "You joined the solo unranked queue!");
            
            // Give leave item
            ItemStack leaveItem = new ItemStack(Material.BED);
            ItemMeta meta = leaveItem.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Leave Queue (Right Click)");
            leaveItem.setItemMeta(meta);
            
            // Save inventory and give item
            player.getInventory().clear();
            player.getInventory().setItem(8, leaveItem);
        }
    }

    public void leaveQueue(Player player) {
        if (soloUnrankedQueue.contains(player.getUniqueId())) {
            soloUnrankedQueue.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "You left the queue!");
            plugin.getSpawnItem().giveSpawnItem(player);
        }
    }

    public void removePlayer(Player player) {
        soloUnrankedQueue.remove(player.getUniqueId());
    }

    private int tickCounter = 0;

    private void startQueueTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if we can start a match every tick
                if (soloUnrankedQueue.size() >= 2) {
                    matchPlayers();
                }

                // Send waiting message every 5 seconds (100 ticks)
                tickCounter++;
                if (tickCounter >= 100) {
                    tickCounter = 0;
                    for (UUID uuid : soloUnrankedQueue) {
                        Player p = org.bukkit.Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendMessage(ChatColor.YELLOW + "Bedfight - Searching for match....");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Run every tick
    }

    private void matchPlayers() {
        UUID p1Id = soloUnrankedQueue.remove(0);
        UUID p2Id = soloUnrankedQueue.remove(0);

        Player p1 = org.bukkit.Bukkit.getPlayer(p1Id);
        Player p2 = org.bukkit.Bukkit.getPlayer(p2Id);

        if (p1 == null || p2 == null) {
            // Re-queue remaining
            if (p1 != null) soloUnrankedQueue.add(0, p1Id);
            if (p2 != null) soloUnrankedQueue.add(0, p2Id);
            return;
        }

        // Get random arena
        List<Arena> arenas = new ArrayList<>(plugin.getBedFightArenaManager().getArenas());
        if (arenas.isEmpty()) {
            p1.sendMessage(ChatColor.RED + "No arenas available.");
            p2.sendMessage(ChatColor.RED + "No arenas available.");
            soloUnrankedQueue.add(0, p2Id);
            soloUnrankedQueue.add(0, p1Id);
            return;
        }
        Arena arena = arenas.get(random.nextInt(arenas.size()));

        p1.sendMessage(ChatColor.GREEN + "Match found! Starting against " + p2.getName() + " on " + arena.getName());
        p2.sendMessage(ChatColor.GREEN + "Match found! Starting against " + p1.getName() + " on " + arena.getName());

        plugin.getBedFightManager().startMatch(arena, p1, p2);
    }
}
