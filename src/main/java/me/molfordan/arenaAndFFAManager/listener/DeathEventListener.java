package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.*;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class DeathEventListener implements Listener {

    private final ArenaManager arenaManager;
    private final ArenaAndFFAManager plugin;
    private final CombatManager combatManager;
    private final DeathMessageManager deathMessageManager;

    public DeathEventListener(ArenaManager arenaManager, ArenaAndFFAManager plugin,
                              CombatManager combatManager, DeathMessageManager deathMessageManager) {

        this.arenaManager = arenaManager;
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.deathMessageManager = deathMessageManager;
    }

    /**
     * UNIFIED VOID FALL DETECTION
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        Location to = event.getTo();
        if (to == null) return;

        Arena arena = plugin.getArenaManager().getArenaByLocationIgnoreY(to);
        if (arena == null) return;

        // Only check Y position for void
        if (to.getY() > arena.getVoidLimit()) return;

        UUID id = player.getUniqueId();

        // Prevent duplicate handling
        if (DeathMessageManager.voidHandled.contains(id)) {
            return;
        }

        // Mark as handled
        DeathMessageManager.voidHandled.add(id);

        // Clear the flag after a short delay (2 seconds should be enough)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DeathMessageManager.voidHandled.remove(id);
        }, 40L); // 40 ticks = 2 seconds

        // For BuildFFA, just set health to 0 and let the natural death event handle it
        if (arena.getType() != ArenaType.FFABUILD) {

            return;
        }

        // For other arena types, handle the death directly
        PlayerStats stats = plugin.getStatsManager().getStats(id);
        stats.resetBuildStreak();
        plugin.getStatsManager().savePlayerAsync(stats);

        // Handle the death with the correct context
        Player killer = plugin.getCombatManager().getAttacker(player);
        deathMessageManager.handleDeath(
                player,
                arena,
                true,  // isVoidDeath
                false  // isQuit
        );
        player.setHealth(0);
    }

    /**
     * BUILD FFA death
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = arenaManager.getArenaByLocation(player.getLocation());
        if (arena == null) return;

        // remove default MC death message
        event.setDeathMessage(null);

        // Only FFABUILD uses normal death (non-void)
        if (arena.getType() != ArenaType.FFABUILD) return;

        // If player died in void, ignore (void is handled in move-event)
        if (player.getLocation().getY() <= arena.getVoidLimit()) return;

        combatManager.clear(player);
        deathMessageManager.clear(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaManager.getArenaByLocation(player.getLocation());
        if (arena == null) return;

        if (arena.getType() == ArenaType.DUEL) return;

        if (arena.getCenter() != null &&
                (arena.getType() == ArenaType.FFA || arena.getType() == ArenaType.FFABUILD)) {

            Location center = arena.getCenter().clone().add(0.5, 0, 0.5);
            event.setRespawnLocation(center);
        }
    }
}
