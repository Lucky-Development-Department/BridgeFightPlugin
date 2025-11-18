package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class BuildFFAListener implements Listener {

    private final ConfigManager configManager;
    private final KitManager kitManager;

    public BuildFFAListener(ConfigManager configManager, KitManager kitManager) {
        this.configManager = configManager;
        this.kitManager = kitManager;
    }

    private boolean isInBuildFFAWorld(Player player) {
        Location loc = configManager.getBuildFFALocation();
        if (loc == null) return false;
        World world = loc.getWorld();
        return player.getWorld().equals(world);
    }

    // ======================================
    // 1) INSTANT RESPAWN (1.8 METHOD)
    // ======================================
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!isInBuildFFAWorld(player)) return;

        // Remove the death screen (1.8 trick)
        Bukkit.getScheduler().runTaskLater(
                ArenaAndFFAManager.getPlugin(),
                () -> player.spigot().respawn(),
                1L
        );
    }

    // ======================================
    // 2) HANDLE RESPAWN LOCATION
    // ======================================
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!isInBuildFFAWorld(player)) return;

        Location buildFFASpawn = configManager.getBuildFFALocation();
        if (buildFFASpawn != null) {
            event.setRespawnLocation(buildFFASpawn);
            player.setGameMode(GameMode.SURVIVAL);

            // Give the kit AFTER respawn
            Bukkit.getScheduler().runTaskLater(
                    ArenaAndFFAManager.getPlugin(),
                    () -> kitManager.applyBuildFFAKit(player),
                    2L
            );
        }
    }
}
