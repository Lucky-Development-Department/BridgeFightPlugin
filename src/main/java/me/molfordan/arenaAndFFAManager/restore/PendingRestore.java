package me.molfordan.arenaAndFFAManager.restore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class PendingRestore {
    private final String arenaName;
    private final String worldName;
    private final int x, y, z;
    private final Material material;
    private final byte data;
    private int timeRemaining; // in seconds

    public PendingRestore(String arenaName, String worldName, int x, int y, int z,
                          Material material, byte data, int timeRemaining) {
        this.arenaName = arenaName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.data = data;
        this.timeRemaining = timeRemaining;
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        return (world != null) ? new Location(world, x, y, z) : null;
    }

    public String getArenaName() { return arenaName; }
    public Material getMaterial() { return material; }
    public byte getData() { return data; }
    public int getTimeRemaining() { return timeRemaining; }

    public void tick() { timeRemaining--; }
    public boolean isExpired() { return timeRemaining <= 0; }
}
