package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.manager.ConfigManager;
import me.molfordan.arenaAndFFAManager.manager.PlatformManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;

public class BridgeFightListener implements Listener {

    private final PlatformManager platformManager;
    private final ConfigManager configManager;

    public BridgeFightListener(PlatformManager platformManager,
                               ConfigManager configManager) {
        this.platformManager = platformManager;
        this.configManager = configManager;
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
            return;
        }

        //.setHealth(0);
    }

    /**
     * Zero damage but allow knockback
     */
    @EventHandler
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        String world = configManager.getBridgeFightWorldName();
        if (world == null) return;

        Player victim = (Player) event.getEntity();
        if (!victim.getWorld().getName().equals(world)) return;

        event.setDamage(0.0);
    }
}
