package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlobalListener implements Listener {

    private final StatsManager statsManager;
    private Set<UUID> flyingPlayers = new HashSet<>();

    public GlobalListener(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onFallDamageGlobal(EntityDamageEvent event){

        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDefaultDeathMessage(PlayerDeathEvent event){
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event){
        Player player = event.getPlayer();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event){
        if (event.toWeatherState()){
            event.setCancelled(true);
            World world = event.getWorld();
            world.setThundering(false);
            world.setStorm(false);
            world.setThunderDuration(0);
            world.setWeatherDuration(0);
            return;
        }

        event.setCancelled(true);

    }

    @EventHandler
    public void onLoadData(PlayerJoinEvent event){
        Player p = event.getPlayer();
        statsManager.loadPlayer(p.getUniqueId(), p.getName());
        if (flyingPlayers.contains(p.getUniqueId())){
            p.setAllowFlight(true);
            p.setFlying(true);
            Location loc = p.getLocation();
            double locY = loc.getY();
            p.teleport(new Location(loc.getWorld(), loc.getX(), locY + 1, loc.getZ()));
        }


    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = e.getPlayer().getUniqueId();
        PlayerStats stats = statsManager.getStats(uuid);
        stats.resetBuildStreak();
        stats.resetBridgeStreak();
        statsManager.savePlayerAsync(statsManager.getStats(uuid));
        e.setQuitMessage(null);
        if (player.isFlying()) {
            flyingPlayers.add(uuid);
        }
    }
    @EventHandler
    public void onBlockFade(BlockFadeEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event){
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event){
        event.setCancelled(true);
    }

}
