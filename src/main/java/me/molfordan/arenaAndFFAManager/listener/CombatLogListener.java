package me.molfordan.arenaAndFFAManager.listener;


import me.molfordan.arenaAndFFAManager.*;
import me.molfordan.arenaAndFFAManager.manager.*;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CombatLogListener implements Listener {

    private final ArenaAndFFAManager plugin;
    private final CombatManager combatManager;
    private final ArenaManager arenaManager;
    private final DeathMessageManager deathMessageManager;
    private final Map<UUID, UUID> quitPlayers = new HashMap<>();
    private final Map<UUID, Arena> lastKnownArena = new HashMap<>();

    private static final Set<String> WHITELISTED_COMMANDS = new HashSet<>(Arrays.asList(
            "/stats",
            "/guistats",
            "/help",
            "/rules",
            "/kit",
            "/hotbarmanager",
            "/hbm"// Example of another allowed command
    ));

    public CombatLogListener(ArenaAndFFAManager plugin, CombatManager combatManager, ArenaManager arenaManager, DeathMessageManager deathMessageManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.arenaManager = arenaManager;
        this.deathMessageManager = deathMessageManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (event.getDamager() instanceof Player) ? (Player) event.getDamager() : null;
        
        // Ignore creative mode attackers and self-damage
        if (attacker == null || attacker.getGameMode() == GameMode.CREATIVE || attacker.equals(victim)) {
            return;
        }

        // Check if both players are in the same arena
        Arena arena = arenaManager.getArenaByLocation(victim.getLocation());
        if (arena == null || arena.getType() == ArenaType.DUEL || arena.getType() == ArenaType.TOPFIGHT) {
            return;
        }

        // Verify both players are actually in the arena
        if (!arena.isInside(attacker.getLocation(), true) || !arena.isInside(victim.getLocation(), true)) {
            return;
        }

        // Tag both players as in combat
        combatManager.tag(victim, attacker, arena);
        combatManager.recordDamage(attacker, victim);
        
        plugin.debug("[Combat] " + attacker.getName() + " hit " + victim.getName() + " in " + arena.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        UUID quitterId = quitter.getUniqueId();

        // Check if player is actually in combat
        if (!combatManager.isInCombat(quitter)) {
            plugin.debug("[CombatLog] " + quitter.getName() + " quit (not in combat)");
            return;
        }

        // Get the attacker and arena
        Player attacker = combatManager.getAttacker(quitter);
        Arena arena = combatManager.getTaggedArena(quitter);
        
        // Try to get arena from attacker if not found on quitter
        if (arena == null && attacker != null) {
            arena = combatManager.getTaggedArena(attacker);
        }
        
        // Last resort: try to get arena from location
        if (arena == null) {
            arena = arenaManager.getArenaByLocation(quitter.getLocation());
            if (arena == null && attacker != null) {
                arena = arenaManager.getArenaByLocation(attacker.getLocation());
            }
        }

        // Still no arena, can't process combat log
        if (arena == null) {
            plugin.debug("[CombatLog] Could not determine arena for " + quitter.getName() + 
                       " (attacker: " + (attacker != null ? attacker.getName() : "none") + ")");
            return;
        }

        // Prevent double handling
        if (quitPlayers.containsKey(quitterId)) {
            plugin.debug("[CombatLog] " + quitter.getName() + " already processed as combat logger");
            return;
        }

        // Log the combat log
        UUID attackerId = (attacker != null) ? attacker.getUniqueId() : null;
        quitPlayers.put(quitterId, attackerId);
        
        String logMsg = "[CombatLog] " + quitter.getName() + " logged out during combat in " + arena.getName();
        if (attacker != null) {
            logMsg += " (last hit by: " + attacker.getName() + ")";
        }
        plugin.debug(logMsg);

        // Handle the combat log death
        deathMessageManager.handleDeath(quitter, arena, false, true);

        // Clean up combat data
        combatManager.clear(quitter);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // If player wasn't stored as a combat logger, ignore
        if (!quitPlayers.containsKey(playerId)) return;

        // Get the arena they were in when they logged out
        Arena arena = combatManager.getTaggedArena(player);
        if (arena == null) {
            // Try to get arena from last known location if not found in combat data
            arena = lastKnownArena.get(playerId);
        }

        // Remove from the tracking map (cleanup only)
        quitPlayers.remove(playerId);
        
        // Note: Death is already handled in handleDeath() called from onPlayerQuit
        // We don't need to add another death here as it would cause duplicate deaths
        if (arena != null) {
            plugin.debug("[CombatLog] Processed combat log for " + player.getName() + " in " + arena.getName());
        }
        
        // Clear any remaining combat state
        combatManager.clear(player);
        player.setHealth(20);
        plugin.debug("[CombatLog] Cleared combat state for " + player.getName() + " on rejoin");
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();

        Arena arena = arenaManager.getArenaByLocation(from);
        if (arena != null) {
            lastKnownArena.put(player.getUniqueId(), arena);
        }
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Don't clear combat state if still in arena world
        if (arenaManager.getArenaByLocation(player.getLocation()) != null) return;

        if (!combatManager.isInCombat(player)) {
            deathMessageManager.clear(player.getUniqueId());
            combatManager.clear(player);
            PlayerKillEventListener.resetStreak(player.getUniqueId());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Extract command without arguments
        String base = msg.split(" ")[0]; // e.g. "/stats"

        // Check whitelist
        boolean isWhitelisted = WHITELISTED_COMMANDS.contains(base);

        if (isWhitelisted) return;

        Arena arena = arenaManager.getArenaByLocation(player.getLocation());
        if (arena == null || arena.getType() == ArenaType.DUEL) return;
        if (!arenaManager.isInArena(player)) return;
        if (!combatManager.isInCombat(player)) return;
        if (combatManager.isBypassing(player.getUniqueId())) return;

        long remaining = (combatManager.getCombatDuration() - (System.currentTimeMillis() - combatManager.getLastHitTime(player))) / 1000;
        remaining = Math.max(0, remaining);

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You're in combat! You can't use commands for " + remaining + " more seconds.");
    }

    @EventHandler
    public void onJoinPlayer(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!combatManager.isBypassing(player.getUniqueId())) {
                        combatManager.toggleBypass(player.getUniqueId());
                        player.sendMessage(ChatColor.GOLD + "Command Bypass during combat logging enabled.");
                    }
                }
            }.runTaskLater(plugin, 35L);
        }
    }
}
