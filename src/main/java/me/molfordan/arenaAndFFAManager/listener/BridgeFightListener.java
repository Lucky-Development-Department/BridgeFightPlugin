package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.manager.DeathMessageManager;
import me.molfordan.arenaAndFFAManager.manager.PlatformManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.UUID;

public class BridgeFightListener implements Listener {

    private final PlatformManager platformManager;
    private final ConfigManager configManager;
    private final ArenaAndFFAManager plugin;
    private String PREFIX;

    public BridgeFightListener(PlatformManager platformManager,
                               ConfigManager configManager, ArenaAndFFAManager plugin) {
        this.platformManager = platformManager;
        this.configManager = configManager;
        this.plugin = plugin;
        this.PREFIX = plugin.getConfigManager().getServerPrefix();
    }

    /**
     * Teleport when falling below platform or into void
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        String worldName = configManager.getBridgeFightWorldName();
        if (worldName == null || !p.getWorld().getName().equals(worldName)) return;

        Location to = event.getTo();
        if (to == null) return;

        // Special handling for Y=0 (Absolute Void)
        if (to.getY() <= 0) {
            handleBridgeVoidFall(p);
            return;
        }

        Arena arena = ArenaAndFFAManager.getPlugin().getArenaManager().getArenaByLocationIgnoreY(p.getLocation());
        if (arena == null || to.getY() > arena.getVoidLimit()) return;

        PlatformRegion region = platformManager.fromLocationIgnoreY(to);
        if (region != null && region.getSpawn() != null) {
            handleBridgeVoidFall(p, region.getSpawn());
        }
    }

    private void handleBridgeVoidFall(Player p) {
        Location spawn = null;
        for (Arena arena : ArenaAndFFAManager.getPlugin().getArenaManager().getArenas()) {
            if (arena.getWorldName().equals(p.getWorld().getName()) && arena.getType() == ArenaType.FFA) {
                if (arena.getCenter() != null) {
                    spawn = arena.getCenter();
                    break;
                }
            }
        }
        
        if (spawn == null) {
            spawn = configManager.getBridgeFightLocation();
        }
        
        handleBridgeVoidFall(p, spawn);
    }

    private void handleBridgeVoidFall(Player p, Location spawn) {
        PlayerStats stats = ArenaAndFFAManager.getPlugin().getStatsManager().getStats(p.getUniqueId());
        
        // Find attacker for message
        Player attacker = plugin.getCombatManager().getAttacker(p);
        String message;
        if (attacker != null && attacker.isOnline() && !attacker.equals(p)) {
            message = ChatColor.RED + p.getName() + ChatColor.GRAY + " was thrown into the void by " + ChatColor.RED + attacker.getName();
        } else {
            message = ChatColor.RED + p.getName() + ChatColor.GRAY + " fell into the void.";
        }
        
        // Broadcast message to players in the same world
        for (Player other : p.getWorld().getPlayers()) {
            other.sendMessage(message);
        }

        if (spawn != null) {
            p.teleport(spawn);
        }
        
        stats.resetBridgeStreak();
        ArenaAndFFAManager.getPlugin().getStatsManager().savePlayerAsync(stats);

        // Notify DeathMessageManager for stats tracking (but it will send its own message, 
        // we might want to suppress it or let it handle stats only)
        // For now, let's just do the manual cleanup to match user request
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setHealth(20.0);
        p.setFireTicks(0);
        p.setFallDistance(0);
        
        plugin.getKitManager().applyBridgeFightKit(p);
        plugin.getEnderPearlListener().removeCooldown(p);
        plugin.getCombatManager().clear(p);
    }

    /**
     * Zero damage but allow knockback
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (!isValidPlayerCombat(event)) return;

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        DeathMessageManager deathManager = plugin.getDeathMessageManager();

        if (handleImmunityChecks(damager, victim, deathManager, event)) return;

        if (!deathManager.handleDuelHit(damager, victim)) {
            event.setCancelled(true);
            return;
        }

        removeDamagerImmunity(damager, deathManager);

        event.setDamage(0.0);
    }

    private boolean isValidPlayerCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return false;
        }

        String world = configManager.getBridgeFightWorldName();
        return world != null && ((Player) event.getEntity()).getWorld().getName().equals(world);
    }

    private boolean handleImmunityChecks(Player damager, Player victim, DeathMessageManager deathManager, EntityDamageByEntityEvent event) {
        // Only cancel if the victim is immune (immune players can damage others)
        if (deathManager.getImmunePlayers().contains(victim.getUniqueId())) {
            event.setCancelled(true);
            damager.sendMessage(formatMessage(" This player has post-fight immunity!", ChatColor.YELLOW));
            return true;
        }

        return false;
    }

    private void removeDamagerImmunity(Player damager, DeathMessageManager deathManager) {
        if (deathManager.getImmunePlayers().contains(damager.getUniqueId())) {
            deathManager.getImmunePlayers().remove(damager.getUniqueId());
            damager.sendMessage(formatMessage(" Your immunity has been removed!", ChatColor.GRAY));
        }
    }

    private String formatMessage(String message, ChatColor color) {
        return ChatColor.translateAlternateColorCodes('&', PREFIX) + color + message;
    }


    @EventHandler
    public void onAnvilInteract(PlayerInteractEvent event){
        if (!(event.getPlayer() instanceof Player)) return;

        String world = configManager.getBridgeFightWorldName();
        if (world == null) return;

        Player player = (Player) event.getPlayer();
        if (!player.getWorld().getName().equals(world)) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK){
            if (event.getClickedBlock().getType() == Material.ANVIL){
                event.setCancelled(true);
            }
        }


    }

    private boolean isInBridgeFightWorld(Player player) {
        Location loc = configManager.getBridgeFightLocation();
        if (loc == null) return false;
        World world = loc.getWorld();
        return player.getWorld().equals(world);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!isInBridgeFightWorld(player)) return;

        Location bridgeFightSpawn = configManager.getBridgeFightLocation();
        if (bridgeFightSpawn != null) {
            event.setRespawnLocation(bridgeFightSpawn);
            player.setGameMode(GameMode.SURVIVAL);

            // Give the kit AFTER respawn
            Bukkit.getScheduler().runTaskLater(
                    ArenaAndFFAManager.getPlugin(),
                    () -> plugin.getKitManager().applyBridgeFightKit(player),
                    2L
            );
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (isInBridgeFightWorld(player)) return;

        // Clear the drops to prevent items from dropping
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true); // Keep the player's inventory

        // Schedule the respawn
        Bukkit.getScheduler().runTaskLater(
                ArenaAndFFAManager.getPlugin(),
                () -> {
                    player.spigot().respawn();
                    // Set the player to survival mode (in case they were in spectator)
                    player.setGameMode(GameMode.SURVIVAL);
                },
                1L
        );
    }
}
