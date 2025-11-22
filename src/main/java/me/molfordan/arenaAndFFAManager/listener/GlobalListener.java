package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
//import me.molfordan.arenaAndFFAManager.utils.FlightManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import org.bukkit.potion.PotionEffect;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlobalListener implements Listener {

    private final StatsManager statsManager;

    private final ArenaAndFFAManager plugin;

    public GlobalListener(StatsManager statsManager, ArenaAndFFAManager plugin) {
        this.statsManager = statsManager;
        this.plugin = plugin;
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
    public void onWorldChange(PlayerChangedWorldEvent event) {
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLoadData(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        statsManager.loadPlayer(p.getUniqueId(), p.getName());
        if (p.hasPermission("luckyessentials.fly")){
            p.setAllowFlight(true);
            p.setFlying(true);
            Location loc = p.getLocation();
            double locY = loc.getY();
            p.teleport(new Location(loc.getWorld(), loc.getX(), locY + 2, loc.getZ()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChanges(PlayerChangedWorldEvent event){
        Player p = event.getPlayer();
        String lobbyWorld = plugin.getConfigManager().getLobbyWorldName();
        if (event.getPlayer().getWorld().equals(lobbyWorld)){
            if (p.hasPermission("luckyessentials.fly")){
                p.setAllowFlight(true);
                p.setFlying(true);
                Location loc = p.getLocation();
                double locY = loc.getY();
                p.teleport(new Location(loc.getWorld(), loc.getX(), locY + 2, loc.getZ()));
            }
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
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
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

    /**
     * @deprecated Use FlightManager directly instead
     */
    @Deprecated
    public static Set<UUID> getFlyingPlayers() {
        // This is a temporary bridge method for backward compatibility
        // You should update any code using this to use FlightManager instead
        return new HashSet<>();
    }
}
