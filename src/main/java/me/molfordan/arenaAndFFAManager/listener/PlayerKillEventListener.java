package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import me.molfordan.arenaAndFFAManager.manager.CombatManager;
import me.molfordan.arenaAndFFAManager.manager.DeathMessageManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerKillEventListener implements Listener {

    private final CombatManager combatManager;
    private final ArenaManager arenaManager;
    private final DeathMessageManager deathMessageManager;
    private final ArenaAndFFAManager plugin;

    private static final Map<UUID, Integer> streakMap = new HashMap<>();

    public PlayerKillEventListener(CombatManager combatManager, ArenaManager arenaManager, DeathMessageManager deathMessageManager, ArenaAndFFAManager plugin) {
        this.combatManager = combatManager;
        this.arenaManager = arenaManager;
        this.deathMessageManager = deathMessageManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // FIXED: only skip if void death was handled earlier
        /*
        if (DeathMessageManager.voidHandled.contains(victim.getUniqueId())) {
            DeathMessageManager.voidHandled.remove(victim.getUniqueId());
            return;
        }

         */

        Player killer = victim.getKiller();
        if (killer == null || killer == victim) {
            killer = combatManager.getAttacker(victim);
        }

        Arena arena = arenaManager.getArenaByLocation(victim.getLocation());
        if (arena == null) return;
        if (arena.getType() == ArenaType.DUEL) return;

        deathMessageManager.handleDeath(victim, arena, false, false);
    }

    /*
    public static int incrementStreak(UUID playerId) {
        PlayerStats stats = ArenaAndFFAManager.getPlugin()
                .getStatsManager()
                .getStats(playerId);

        if (stats == null) return 0;

        int current;

        // Determine arena type (bridge or build) from cached stats
        // This must match whatever arena player is currently in
        Arena arena = ArenaAndFFAManager.getPlugin().getArenaManager().getArenaByLocation(Bukkit.getPlayer(playerId).getLocation());

        if (arena == null) return 0;

        if (arena.getType() == ArenaType.FFA) {
            current = stats.getCurrentBridgeKillstreak() + 1;
            stats.setCurrentBridgeKillstreak(current);

            if (current > stats.getHighestBridgeKillstreak()) {
                stats.setHighestBridgeKillstreak(current);
            }
        } else if (arena.getType() == ArenaType.FFABUILD) {
            current = stats.getCurrentBuildKillstreak() + 1;
            stats.setCurrentBuildKillstreak(current);

            if (current > stats.getHighestBuildKillstreak()) {
                stats.setHighestBuildKillstreak(current);
            }
        } else {
            return 0;
        }

        ArenaAndFFAManager.getPlugin().getStatsManager().savePlayerAsync(stats);
        return current;
    }

     */


    public static void resetStreak(UUID playerId) {
        streakMap.remove(playerId);
    }

    public static int getStreak(UUID playerId) {
        return streakMap.getOrDefault(playerId, 0);
    }

    public static boolean hasStreak(UUID playerId) {
        return streakMap.containsKey(playerId);
    }

    public static void setStreak(UUID playerId, int streak) {
        streakMap.put(playerId, streak);
    }
}
