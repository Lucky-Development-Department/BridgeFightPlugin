package me.molfordan.bridgefightplugin.manager;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoRestartManager {
    
    private final BridgeFightPlugin plugin;
    private final ConfigManager configManager;
    private BukkitRunnable restartScheduler;
    private BukkitRunnable countdownTask;
    private final AtomicBoolean restartInProgress = new AtomicBoolean(false);
    
    public AutoRestartManager(BridgeFightPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    public void start() {
        if (!configManager.isAutoRestartEnabled()) {
            return;
        }
        
        stop(); // Stop any existing scheduler
        
        restartScheduler = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndScheduleRestart();
            }
        };
        
        // Check every minute
        restartScheduler.runTaskTimer(plugin, 0L, 20L * 60L);
        
        plugin.getLogger().info("Auto-restart system started");
    }
    
    public void stop() {
        if (restartScheduler != null) {
            restartScheduler.cancel();
            restartScheduler = null;
        }
        
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        restartInProgress.set(false);
        plugin.getLogger().info("Auto-restart system stopped");
    }
    
    private void checkAndScheduleRestart() {
        if (restartInProgress.get()) {
            return;
        }
        
        try {
            String restartTime = configManager.getAutoRestartTime();
            String timezone = configManager.getAutoRestartTimezone();
            
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            ZonedDateTime targetTime = parseRestartTime(restartTime, now);
            
            // Check if we're within 1 minute of restart time
            if (now.isAfter(targetTime.minusMinutes(1)) && now.isBefore(targetTime.plusMinutes(1))) {
                scheduleRestart(60); // 60 seconds countdown
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking auto-restart time: " + e.getMessage());
        }
    }
    
    private ZonedDateTime parseRestartTime(String timeString, ZonedDateTime now) throws DateTimeParseException {
        // Parse formats like "1AM", "1:30AM", "13:00", "01:00"
        DateTimeFormatter formatter;
        
        if (timeString.matches("\\d+AM|\\d+PM")) {
            formatter = DateTimeFormatter.ofPattern("ha");
        } else if (timeString.matches("\\d+:\\d+AM|\\d+:\\d+PM")) {
            formatter = DateTimeFormatter.ofPattern("h:ma");
        } else if (timeString.matches("\\d+:\\d+")) {
            formatter = DateTimeFormatter.ofPattern("H:mm");
        } else if (timeString.matches("\\d+")) {
            formatter = DateTimeFormatter.ofPattern("H");
        } else {
            throw new DateTimeParseException("Invalid time format: " + timeString, timeString, 0);
        }
        
        LocalTime time = LocalTime.parse(timeString.toUpperCase(), formatter);
        return now.with(time).withSecond(0).withNano(0);
    }
    
    private void scheduleRestart(int secondsUntilRestart) {
        if (restartInProgress.compareAndSet(false, true)) {
            
            final int[] countdown = {secondsUntilRestart};
            
            countdownTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (countdown[0] <= 0) {
                        performRestart();
                        cancel();
                        return;
                    }
                    
                    if (countdown[0] == 60 || countdown[0] == 30 || countdown[0] == 10 || countdown[0] <= 5) {
                        broadcastRestartWarning(countdown[0]);
                    }
                    
                    countdown[0]--;
                }
            };
            
            countdownTask.runTaskTimer(plugin, 0L, 20L);
            
            plugin.getLogger().info("Server restart scheduled in " + secondsUntilRestart + " seconds");
        }
    }
    
    private void broadcastRestartWarning(int seconds) {
        String message;
        ChatColor color;
        
        if (seconds <= 10) {
            color = ChatColor.RED;
            message = "§c§lSERVER RESTARTING IN " + seconds + " SECONDS!";
        } else if (seconds <= 30) {
            color = ChatColor.YELLOW;
            message = "§eServer restarting in " + seconds + " seconds!";
        } else {
            color = ChatColor.AQUA;
            message = "§bServer restarting in " + (seconds / 60) + " minute(s)!";
        }
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(color + "§l" + message);
        Bukkit.broadcastMessage("");
    }
    
    private void performRestart() {
        plugin.getLogger().info("Performing automatic server restart...");

        // Perform block cleanup and map restoration
        cleanupBlocksAndRestore();
        
        // Kick all players with restart message
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.kickPlayer("§c§lServer is restarting!\n§ePlease rejoin in a moment.");
        });
        
        // Save all data
        Bukkit.savePlayers();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
        
        // Schedule the actual restart after a short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                System.exit(0); // This will trigger the server restart script
            }
        }.runTaskLater(plugin, 20L);
    }

    private void cleanupBlocksAndRestore() {
        plugin.getLogger().info("Cleaning up player blocks and restoring maps...");

        // 1. Remove blocks with player_blocks and egg_bridge_block metadata in all worlds
        for (World world : Bukkit.getWorlds()) {
            // This is computationally expensive, but acceptable during a server restart
            // since players are about to be kicked and server is shutting down.
            // We iterate through loaded chunks for efficiency.
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                int minX = chunk.getX() << 4;
                int minZ = chunk.getZ() << 4;
                int maxX = minX + 15;
                int maxZ = minZ + 15;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = 0; y < world.getMaxHeight(); y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.hasMetadata("player_blocks") || block.hasMetadata("egg_bridge_block")) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }

        // 2. Execute arenamap restore command if configured
        String mapToRestore = configManager.getAutoRestoreMap();
        if (mapToRestore != null && !mapToRestore.equalsIgnoreCase("none")) {
            plugin.getLogger().info("Restoring map: " + mapToRestore);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "arenamap " + mapToRestore + " restore");
        }
    }
    
    public boolean isRestartInProgress() {
        return restartInProgress.get();
    }
    
    public int getTimeUntilRestart() {
        if (!configManager.isAutoRestartEnabled() || restartInProgress.get()) {
            return -1;
        }
        
        try {
            String restartTime = configManager.getAutoRestartTime();
            String timezone = configManager.getAutoRestartTimezone();
            
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            ZonedDateTime targetTime = parseRestartTime(restartTime, now);
            
            // If target time has passed today, schedule for tomorrow
            if (now.isAfter(targetTime)) {
                targetTime = targetTime.plusDays(1);
            }
            
            return (int) Duration.between(now, targetTime).getSeconds();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error calculating time until restart: " + e.getMessage());
            return -1;
        }
    }
}
