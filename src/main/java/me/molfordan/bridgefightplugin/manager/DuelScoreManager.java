package me.molfordan.bridgefightplugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelScoreManager {
    private final Plugin plugin;
    private final Map<UUID, Integer> redScores = new HashMap<>();
    private final Map<UUID, Integer> blueScores = new HashMap<>();
    private final Map<UUID, BukkitRunnable> resetTasks = new HashMap<>();
    private final Map<UUID, Long> matchEndTimes = new HashMap<>();
    // Tracks who a player has requested a rematch from
    private final Map<UUID, UUID> pendingRematchRequests = new HashMap<>(); 

    public DuelScoreManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void setRematchRequest(UUID from, UUID to) {
        pendingRematchRequests.put(from, to);
    }
    
    public boolean hasRematchRequest(UUID from, UUID to) {
        return pendingRematchRequests.containsKey(from) && pendingRematchRequests.get(from).equals(to);
    }
    
    public void removeRematchRequest(UUID from) {
        pendingRematchRequests.remove(from);
    }
    
    public void setMatchEndTime(UUID duelId) {
        matchEndTimes.put(duelId, System.currentTimeMillis());
    }

    public boolean isRematchAllowed(UUID duelId) {
        return matchEndTimes.containsKey(duelId) && (System.currentTimeMillis() - matchEndTimes.get(duelId) < 30000);
    }

    public void incrementScore(String team, UUID player1, UUID player2) {
        UUID duelId = getDuelId(player1, player2);
        Map<UUID, Integer> scores = team.equalsIgnoreCase("RED") ? redScores : blueScores;
        scores.put(duelId, scores.getOrDefault(duelId, 0) + 1);
        
        // Cancel existing reset task if any
        if (resetTasks.containsKey(duelId)) {
            resetTasks.get(duelId).cancel();
        }
    }

    public int getScore(String team, UUID player1, UUID player2) {
        UUID duelId = getDuelId(player1, player2);
        return (team.equalsIgnoreCase("RED") ? redScores : blueScores).getOrDefault(duelId, 0);
    }
    
    // Helper to generate a consistent ID for the pair of players
    private UUID getDuelId(UUID p1, UUID p2) {
        return UUID.nameUUIDFromBytes((p1.toString() + p2.toString()).getBytes());
    }

    public void startResetTimer(UUID duelId) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                redScores.remove(duelId);
                blueScores.remove(duelId);
                resetTasks.remove(duelId);
            }
        };
        task.runTaskLater(plugin, 30L * 20L); // 30 seconds
        resetTasks.put(duelId, task);
    }
    
    public void cancelResetTimer(UUID duelId) {
        if (resetTasks.containsKey(duelId)) {
            resetTasks.get(duelId).cancel();
            resetTasks.remove(duelId);
        }
    }
}
