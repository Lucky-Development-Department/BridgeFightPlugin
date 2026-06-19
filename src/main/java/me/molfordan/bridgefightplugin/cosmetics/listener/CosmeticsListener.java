package me.molfordan.bridgefightplugin.cosmetics.listener;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.cosmetics.CosmeticsManager;
import me.molfordan.bridgefightplugin.cosmetics.objects.Trail;
import me.molfordan.bridgefightplugin.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CosmeticsListener implements Listener {

    private final BridgeFightPlugin plugin;
    private final CosmeticsManager cosmeticsManager;
    private final Map<Projectile, Trail> activeProjectiles = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Trail> playerTrailCache = new ConcurrentHashMap<>();

    public CosmeticsListener(BridgeFightPlugin plugin, CosmeticsManager cosmeticsManager) {
        this.plugin = plugin;
        this.cosmeticsManager = cosmeticsManager;
        startTrailTask();
    }

    private void startTrailTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Projectile Trails
                Iterator<Map.Entry<Projectile, Trail>> it = activeProjectiles.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Projectile, Trail> entry = it.next();
                    Projectile projectile = entry.getKey();
                    Trail trail = entry.getValue();

                    if (!projectile.isValid() || projectile.isDead() || projectile.isOnGround()) {
                        it.remove();
                        continue;
                    }

                    if (trail.getEffect() != null) {
                        projectile.getWorld().spigot().playEffect(
                                projectile.getLocation(),
                                trail.getEffect(),
                                0, 0,
                                0.05f, 0.05f, 0.05f,
                                0.0f,
                                1,
                                32
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Changed to 1 tick for smoother projectile trails
    }

    public void updatePlayerTrailCache(Player player) {
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) {
            playerTrailCache.remove(player.getUniqueId());
            return;
        }

        String trailId = stats.getSelectedTrail();
        if (trailId == null || trailId.equalsIgnoreCase("none")) {
            playerTrailCache.remove(player.getUniqueId());
            return;
        }

        Trail trail = cosmeticsManager.getTrail(trailId);
        if (trail != null && trail.getEffect() != null) {
            playerTrailCache.put(player.getUniqueId(), trail);
        } else {
            playerTrailCache.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player shooter = (Player) event.getEntity().getShooter();
        
        Trail trail = playerTrailCache.get(shooter.getUniqueId());
        if (trail == null) {
            updatePlayerTrailCache(shooter);
            trail = playerTrailCache.get(shooter.getUniqueId());
        }

        if (trail != null && trail.getEffect() != null) {
            activeProjectiles.put(event.getEntity(), trail);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        activeProjectiles.remove(event.getEntity());
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        playerTrailCache.remove(event.getPlayer().getUniqueId());
    }
}
