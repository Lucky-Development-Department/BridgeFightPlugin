package me.molfordan.arenaAndFFAManager.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportPendingManager {

    private final Map<UUID, Location> waiting = new HashMap<>();

    public void add(Player player) {
        waiting.put(player.getUniqueId(), player.getLocation().clone());
    }

    public void remove(Player player) {
        waiting.remove(player.getUniqueId());
    }

    public boolean isWaiting(Player player) {
        return waiting.containsKey(player.getUniqueId());
    }

    public boolean hasMoved(Player player) {
        Location original = waiting.get(player.getUniqueId());
        if (original == null) return false;

        Location now = player.getLocation();

        return original.getBlockX() != now.getBlockX()
                || original.getBlockY() != now.getBlockY()
                || original.getBlockZ() != now.getBlockZ();
    }
}
