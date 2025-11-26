package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.manager.StatsManager;
//import me.molfordan.arenaAndFFAManager.utils.FlightManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class GlobalListener implements Listener {

    private final StatsManager statsManager;

    private long lastWeatherCommand = 0;

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
    public void onWeatherChange(WeatherChangeEvent event) {
        // If a weather command was executed in the last 300 ms, allow it
        if (System.currentTimeMillis() - lastWeatherCommand < 300) {
            return;
        }

        // Otherwise block natural weather
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/weather")) {
            lastWeatherCommand = System.currentTimeMillis();
        }
    }
    /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void debugBlockPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) {
            Player p = e.getPlayer();
            Location l = e.getBlock().getLocation();

            p.sendMessage("§c[DEBUG] BlockPlace cancelled by: "
                    + getCancellingPlugins(BlockPlaceEvent.class)
                    + " at " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
        }
    }

    private String getCancellingPlugins(Class<? extends Event> eventClass) {
        HandlerList handlers = null;
        try {
            handlers = (HandlerList) eventClass.getMethod("getHandlerList").invoke(null);
        } catch (Exception ex) {
            return "Unknown";
        }

        List<String> list = new ArrayList<>();
        for (RegisteredListener rl : handlers.getRegisteredListeners()) {
            list.add(rl.getPlugin().getName());
        }
        return String.join(", ", list);
    }

     */

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
