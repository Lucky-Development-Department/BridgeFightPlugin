package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.kits.KitManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler
    public void onDrinkPotion(PlayerItemConsumeEvent event){
        Player player = event.getPlayer();
        if (event.getItem().getType() != Material.POTION) return;

        Bukkit.getScheduler().runTaskLater(ArenaAndFFAManager.getPlugin(), () ->
                minusAmount(event.getPlayer(), new ItemStack(Material.GLASS_BOTTLE), 1), 5L);
    }

    public void minusAmount(Player p, ItemStack i, int amount) {
        if (i.getAmount() - amount <= 0) {
            p.getInventory().removeItem(i);
            return;
        }
        i.setAmount(i.getAmount() - amount);
        p.updateInventory();
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
