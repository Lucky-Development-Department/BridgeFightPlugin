package me.molfordan.bridgefightplugin.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.manager.ConfigManager;
import me.molfordan.bridgefightplugin.manager.DeathMessageManager;
import me.molfordan.bridgefightplugin.manager.PlatformManager;
import me.molfordan.bridgefightplugin.object.Arena;
import me.molfordan.bridgefightplugin.object.PlatformRegion;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import me.molfordan.bridgefightplugin.object.enums.ArenaType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class BridgeFightListener implements Listener {

    private final PlatformManager platformManager;
    private final ConfigManager configManager;
    private final BridgeFightPlugin plugin;
    private String PREFIX;

    public BridgeFightListener(PlatformManager platformManager,
                               ConfigManager configManager, BridgeFightPlugin plugin) {
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

        Arena arena = BridgeFightPlugin.getPlugin().getArenaManager().getArenaByLocationIgnoreY(p.getLocation());
        if (arena == null || to.getY() > arena.getVoidLimit()) return;

        PlatformRegion region = platformManager.fromLocationIgnoreY(to);
        if (region != null && region.getSpawn() != null) {
            handleBridgeVoidFall(p, region.getSpawn());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();

        String worldName = configManager.getBridgeFightWorldName();
        if (worldName == null || !p.getWorld().getName().equals(worldName)) return;

        // Check if damage is caused by void or if the player is at or below Y=0
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID || p.getLocation().getY() <= 0) {
            event.setCancelled(true);
            handleBridgeVoidFall(p);
        }
    }

    private void handleBridgeVoidFall(Player p) {
        Location spawn = null;
        for (Arena arena : BridgeFightPlugin.getPlugin().getArenaManager().getArenas()) {
            if (arena.getWorldName().equals(p.getWorld().getName()) && arena.getType() == ArenaType.FFA) {
                if (arena.getCenter() != null) {
                    spawn = arena.getCenter();
                    // Ensure the spawn location has a valid world
                    if (spawn.getWorld() == null) {
                        spawn = spawn.clone();
                        spawn.setWorld(Bukkit.getWorld(arena.getWorldName()));
                    }
                    break;
                }
            }
        }
        
        if (spawn == null || spawn.getWorld() == null) {
            spawn = configManager.getBridgeFightLocation();
        }
        
        handleBridgeVoidFall(p, spawn);
    }

    private void handleBridgeVoidFall(Player p, Location spawn) {
        // Find attacker for stats tracking
        Player attacker = plugin.getCombatManager().getAttacker(p);
        Arena arena = BridgeFightPlugin.getPlugin().getArenaManager().getArenaByLocation(p.getLocation());
        PlatformRegion region = platformManager.fromLocationIgnoreY(p.getLocation());

        // Use DeathMessageManager to handle stats and messages
        // We pass 'true' for isVoidDeath
        plugin.getDeathMessageManager().handleDeath(p, arena, true, false);

        if (spawn != null && spawn.getWorld() != null) {
            p.teleport(spawn);
        } else {
            // Last resort: Teleport to world spawn or log error
            p.teleport(p.getWorld().getSpawnLocation());
        }
        
        // Manual cleanup to match existing mechanics
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setHealth(20.0);
        p.setFireTicks(0);
        p.setFallDistance(0);
        if (region == null){
            plugin.getSpawnItem().giveBridgeFightSpawnItem(p);
            return;
        }
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
                    BridgeFightPlugin.getPlugin(),
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
                BridgeFightPlugin.getPlugin(),
                () -> {
                    player.spigot().respawn();
                    // Set the player to survival mode (in case they were in spectator)
                    player.setGameMode(GameMode.SURVIVAL);
                },
                1L
        );
    }
}
