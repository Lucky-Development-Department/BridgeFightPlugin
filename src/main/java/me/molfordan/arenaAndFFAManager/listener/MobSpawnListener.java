package me.molfordan.arenaAndFFAManager.listener;

import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.manager.ArenaManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobSpawnListener implements Listener {

    private final ArenaManager arenaManager;

    public MobSpawnListener(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        Location spawnLoc = event.getLocation();

        for (Arena arena : arenaManager.getAllArenas()) {
            if (arena.getPos1() == null || arena.getPos2() == null) continue;
            if (!arena.getPos1().getWorld().equals(spawnLoc.getWorld())) continue;

            int x = spawnLoc.getBlockX();
            int y = spawnLoc.getBlockY();
            int z = spawnLoc.getBlockZ();

            int minX = Math.min(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int maxX = Math.max(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int minY = Math.min(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
            int maxY = Math.max(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
            int minZ = Math.min(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());
            int maxZ = Math.max(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());

            if (x >= minX && x <= maxX &&
                    y >= minY && y <= maxY &&
                    z >= minZ && z <= maxZ) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
