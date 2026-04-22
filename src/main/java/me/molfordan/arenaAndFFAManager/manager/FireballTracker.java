package me.molfordan.arenaAndFFAManager.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FireballTracker {
    
    private final Map<UUID, UUID> fireballOwners = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fireballTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> recentlyHitByFireball = new ConcurrentHashMap<>();
    
    public void trackFireball(Fireball fireball, Player owner) {
        fireballOwners.put(fireball.getUniqueId(), owner.getUniqueId());
        fireballTimes.put(fireball.getUniqueId(), System.currentTimeMillis());
        
        // Clean up old fireballs after 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                fireballOwners.remove(fireball.getUniqueId());
                fireballTimes.remove(fireball.getUniqueId());
            }
        }.runTaskLater(owner.getServer().getPluginManager().getPlugin("ArenaAndFFAManager"), 20L * 30L);
    }
    
    public void markPlayerHitByFireball(Player player, Fireball fireball) {
        recentlyHitByFireball.put(player.getUniqueId(), true);
        
        // Remove the hit flag after 3 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                recentlyHitByFireball.remove(player.getUniqueId());
            }
        }.runTaskLater(player.getServer().getPluginManager().getPlugin("ArenaAndFFAManager"), 20L * 3L);
    }
    
    public boolean wasRecentlyHitByFireball(Player player) {
        return recentlyHitByFireball.containsKey(player.getUniqueId());
    }
    
    public Player getFireballOwner(Fireball fireball) {
        UUID ownerId = fireballOwners.get(fireball.getUniqueId());
        if (ownerId == null) return null;
        
        // Check if fireball is too old (more than 30 seconds)
        Long fireballTime = fireballTimes.get(fireball.getUniqueId());
        if (fireballTime == null || (System.currentTimeMillis() - fireballTime) > 30000) {
            fireballOwners.remove(fireball.getUniqueId());
            fireballTimes.remove(fireball.getUniqueId());
            return null;
        }
        
        return Bukkit.getPlayer(ownerId);
    }
    
    public void cleanup() {
        fireballOwners.clear();
        fireballTimes.clear();
        recentlyHitByFireball.clear();
    }
}
