package me.molfordan.bridgefightplugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TNTTracker {
    
    private final Map<UUID, UUID> tntOwners = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tntTimes = new ConcurrentHashMap<>();
    
    public void trackTNT(TNTPrimed tnt, Player owner) {
        tntOwners.put(tnt.getUniqueId(), owner.getUniqueId());
        tntTimes.put(tnt.getUniqueId(), System.currentTimeMillis());
        
        // Clean up tracking after 30 seconds if it hasn't exploded
        new BukkitRunnable() {
            @Override
            public void run() {
                tntOwners.remove(tnt.getUniqueId());
                tntTimes.remove(tnt.getUniqueId());
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("ArenaAndFFAManager"), 20L * 30L);
    }
    
    public Player getTNTOwner(TNTPrimed tnt) {
        UUID ownerId = tntOwners.get(tnt.getUniqueId());
        if (ownerId == null) return null;
        
        // Check if TNT is too old (more than 30 seconds)
        Long tntTime = tntTimes.get(tnt.getUniqueId());
        if (tntTime == null || (System.currentTimeMillis() - tntTime) > 30000) {
            tntOwners.remove(tnt.getUniqueId());
            tntTimes.remove(tnt.getUniqueId());
            return null;
        }
        
        return Bukkit.getPlayer(ownerId);
    }

    public void removeTNT(UUID tntId) {
        tntOwners.remove(tntId);
        tntTimes.remove(tntId);
    }
    
    public void cleanup() {
        tntOwners.clear();
        tntTimes.clear();
    }
}
