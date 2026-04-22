package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.manager.DeathMessageManager;
import me.molfordan.arenaAndFFAManager.manager.PlatformManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
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
     * Teleport when falling below platform
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        String world = configManager.getBridgeFightWorldName();
        if (world == null || !p.getWorld().getName().equals(world)) return;

        Location to = event.getTo();
        if (to == null) return;

        Arena arena = ArenaAndFFAManager.getPlugin().getArenaManager().getArenaByLocationIgnoreY(p.getLocation());
        if (arena == null || to.getY() > arena.getVoidLimit()) return;

        PlatformRegion region = platformManager.fromLocationIgnoreY(to);
        PlayerStats stats = ArenaAndFFAManager.getPlugin().getStatsManager().getStats(p.getUniqueId());

        if (region != null && region.getSpawn() != null) {
            p.teleport(region.getSpawn());
            stats.resetBridgeStreak();
            ArenaAndFFAManager.getPlugin().getStatsManager().savePlayerAsync(stats);

            // Add death message handling
            ArenaAndFFAManager.getPlugin().getDeathMessageManager().handleDeath(
                    p,
                    arena,
                    true,  // isVoidDeath = true
                    false  // isQuit = false
            );
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            plugin.getKitManager().applyBridgeFightKit(p);
            return;
        }

        //.setHealth(0);
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
