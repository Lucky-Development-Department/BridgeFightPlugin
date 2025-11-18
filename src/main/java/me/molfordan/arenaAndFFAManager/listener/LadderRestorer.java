package me.molfordan.arenaAndFFAManager.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LadderRestorer {

    private final Map<String, Byte> trackedLadders = new ConcurrentHashMap<>();

    public void trackLadder(Location location, byte data) {
        String locKey = getLocKey(location);
        trackedLadders.put(locKey, data);
    }

    public void untrackLadder(Location location) {
        String locKey = getLocKey(location);
        trackedLadders.remove(locKey);
    }

    public boolean isTracked(Location location) {
        String locKey = getLocKey(location);
        return trackedLadders.containsKey(locKey);
    }

    public byte getTrackedData(Location location) {
        String locKey = getLocKey(location);
        return trackedLadders.getOrDefault(locKey, (byte) 0);
    }

    public BlockFace getAttachedFace(byte ladderData) {
        switch (ladderData) {
            case 2: return BlockFace.NORTH;
            case 3: return BlockFace.SOUTH;
            case 4: return BlockFace.WEST;
            case 5: return BlockFace.EAST;
            default: return BlockFace.SELF;
        }
    }

    public void deferLadderUntilSupportRestored(Location ladderLoc, byte ladderData, Plugin plugin) {
        new BukkitRunnable() {
            int retries = 30;

            @Override
            public void run() {
                if (retries-- <= 0) {
                    cancel();
                    return;
                }

                BlockFace attachedFace = getAttachedFace(ladderData);
                Block support = ladderLoc.getBlock().getRelative(attachedFace.getOppositeFace());

                if (support.getType().isSolid()) {
                    Block block = ladderLoc.getBlock();
                    block.setType(Material.LADDER);
                    block.setData(ladderData);
                    trackLadder(ladderLoc, ladderData);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every 1 second for up to 30 seconds
    }

    private String getLocKey(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
