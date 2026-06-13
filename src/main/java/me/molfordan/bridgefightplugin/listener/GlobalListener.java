package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.ArenaManager;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import me.molfordan.bridgefightplugin.manager.StatsManager;
//import me.molfordan.arenaAndFFAManager.utils.FlightManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class GlobalListener implements Listener {

    private final StatsManager statsManager;

    private long lastWeatherCommand = 0;

    private final BridgeFightPlugin plugin;
    private final ArenaManager arenaManager;

    public GlobalListener(StatsManager statsManager, BridgeFightPlugin plugin, ArenaManager arenaManager) {
        this.statsManager = statsManager;
        this.plugin = plugin;

        this.arenaManager = arenaManager;
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
        
        // 1. Queue check must be first and exclusive to ensure it overrides everything
        if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            plugin.getMatchmakingService().giveLeaveItem(player);
            return;
        }

        // 2. Otherwise restore party or spawn items
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        if (plugin.getPartyManager().isInParty(player)) {
            plugin.getPartyManager().givePartyItems(player);
        } else {
            plugin.getSpawnItem().giveSpawnItem(player);
        }
    }

    @EventHandler
    public void onPlayerIsQueueing(EntityDamageEvent event){
        Player player = (Player) event.getEntity();
        if (plugin.getMatchmakingService().isInWaitingQueue(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerIsQueueing(EntityDamageByEntityEvent event){
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        
        String buildFFAWorld = plugin.getConfigManager().getBuildFFAWorldName();
        boolean inBuildFFA = victim.getWorld().getName().equals(buildFFAWorld);
        
        // Only apply in BuildFFA world or based on global policy
        if (!inBuildFFA) return;

        boolean damagerQueued = plugin.getMatchmakingService().isInWaitingQueue(damager.getUniqueId());
        boolean victimQueued = plugin.getMatchmakingService().isInWaitingQueue(victim.getUniqueId());

        if (damagerQueued) {
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "You're in bedfight queue! you can't damage other players!");
            victim.sendMessage(ChatColor.RED + "This player is in bedfight queue!");
        } else if (victimQueued) {
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "This player is in bedfight queue!");
        }
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
        Player player = event.getPlayer();
        if (player.isOp()) return; // Admins are always exempt

        me.molfordan.bridgefightplugin.bedfight.BedFightSession session = plugin.getBedFightManager().getSession(player);
        if (session == null) return;
        if (session.isSpectator(player.getUniqueId())) return; // Allow spectators to use commands

        String cmd = event.getMessage().toLowerCase();
        String command = cmd.split(" ")[0];

        // Whitelist: commands allowed during a match
        Set<String> allowed = new HashSet<>(Arrays.asList(
            "/kiteditor", "/forfeit", "/leave", "/duel", "/bfparty", "/bfp", "/queue"
        ));

        if (!allowed.contains(command) || !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use that command during a BedFight match!");
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
            p.sendTitle("", "");
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
    public void onCrafting(PrepareItemCraftEvent e){
        if (e == null) return;
        e.getInventory().setResult(new ItemStack(Material.AIR));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = e.getPlayer().getUniqueId();
        
        // Remove from queue
        plugin.getMatchmakingService().removeFromQueue(player);
        
        PlayerStats stats = statsManager.getStats(uuid);
        if (stats != null) {
            stats.resetBuildStreak();
            stats.resetBridgeStreak();
            statsManager.savePlayerAsync(stats);
        }
        
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
    public void onExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    @EventHandler
    public void onExpBottle(ExpBottleEvent event) {
        event.setExperience(0);
    }

    @EventHandler
    public void onExpSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.EXPERIENCE_ORB) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event){
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event){
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        
        // Always allow drops in BedFight match worlds
        if (worldName.startsWith("bf_")) {
            return;
        }

        // Block drops if in BuildFFA or BridgeFight
        String buildFFAWorld = plugin.getConfigManager().getBuildFFAWorldName();
        String bridgeFightWorld = plugin.getConfigManager().getBridgeFightWorldName();

        if ((buildFFAWorld != null && worldName.equals(buildFFAWorld)) ||
            (bridgeFightWorld != null && worldName.equals(bridgeFightWorld))) {
            event.setCancelled(true);
        }
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
