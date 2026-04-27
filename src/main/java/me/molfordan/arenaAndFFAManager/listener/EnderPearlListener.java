package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderPearlListener implements Listener {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> activeTasks = new HashMap<>();
    private static final long COOLDOWN_SECONDS = 5;
    private static final long COOLDOWN_MILLIS = COOLDOWN_SECONDS * 1000;
    private final ArenaAndFFAManager plugin;

    public EnderPearlListener(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseEnderPearl(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(id)) {
            long last = cooldowns.get(id);
            long remaining = (last + COOLDOWN_MILLIS) - now;

            if (remaining > 0) {
                event.setCancelled(true);

                // Restore pearl if client consumed it early
                //player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                player.updateInventory();

                double sec = remaining / 1000.0;
                player.sendMessage("§cYou must wait §e" + String.format("%.1f", sec) + "s §cbefore using another Ender Pearl!");
                return;
            }
        }

        // Register cooldown start
        cooldowns.put(id, now);

        // Cancel previous task if somehow still running
        if (activeTasks.containsKey(id)) {
            activeTasks.get(id).cancel();
        }

        // EXP Bar Countdown Task (100 ticks = 5 seconds)
        org.bukkit.scheduler.BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            private int ticksLeft = 100;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(id);
                
                // If player leaves, stop the task
                if (p == null || !p.isOnline()) {
                    cooldowns.remove(id);
                    activeTasks.remove(id);
                    this.cancel();
                    return;
                }

                // Finish countdown
                if (ticksLeft <= 0) {
                    p.setExp(0.0f);
                    p.setLevel(0);
                    p.sendMessage("§aYou can now use your Ender Pearl again!");
                    cooldowns.remove(id);
                    activeTasks.remove(id);
                    this.cancel();
                    return;
                }

                // Update EXP bar (1.0 to 0.0) and Level (5 to 1)
                float progress = (float) ticksLeft / 100.0f;
                p.setExp(progress);
                p.setLevel((int) Math.ceil(ticksLeft / 20.0));

                ticksLeft--;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeTasks.put(id, task);
    }

    public static long getCooldownSeconds() { return COOLDOWN_SECONDS; }
    public static long getCooldownMillis() { return COOLDOWN_MILLIS; }
    public static Map<UUID, Long> getCooldowns() { return cooldowns; }

    /**
     * Destroys the EnderPearlListener by cleaning up all active cooldowns
     * and resetting player EXP/levels for any active countdowns.
     */
    public void destroy() {
        // Reset EXP and levels for all players with active cooldowns
        for (UUID id : new HashMap<>(cooldowns).keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.setExp(0.0f);
                player.setLevel(0);
            }
        }
        
        // Cancel all tasks
        for (org.bukkit.scheduler.BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        
        // Clear all cooldowns
        cooldowns.clear();
    }

    /**
     * Removes cooldown for a specific player and resets their EXP/level
     * @param player The player to remove cooldown for
     */
    public void removeCooldown(Player player) {
        if (player == null) return;
        
        UUID id = player.getUniqueId();
        if (cooldowns.containsKey(id)) {
            cooldowns.remove(id);
            
            // Cancel the display task immediately
            if (activeTasks.containsKey(id)) {
                activeTasks.get(id).cancel();
                activeTasks.remove(id);
            }

            player.setExp(0.0f);
            player.setLevel(0);
        }
    }
}
