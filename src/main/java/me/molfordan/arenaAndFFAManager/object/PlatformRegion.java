package me.molfordan.arenaAndFFAManager.object;

import org.bukkit.Location;

public class PlatformRegion {
    private Location pos1;
    private Location pos2;
    private Location spawn;

    public PlatformRegion(Location pos1, Location pos2, Location spawn) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.spawn = spawn;
    }

    public void setPos1(Location loc) { this.pos1 = loc; }
    public void setPos2(Location loc) { this.pos2 = loc; }
    public void setSpawn(Location loc) { this.spawn = loc; }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && spawn != null;
    }

    // Normal full cuboid (X/Y/Z)
    public boolean isInside(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        double x1 = Math.min(pos1.getX(), pos2.getX());
        double x2 = Math.max(pos1.getX(), pos2.getX());
        double y1 = Math.min(pos1.getY(), pos2.getY());
        double y2 = Math.max(pos1.getY(), pos2.getY());
        double z1 = Math.min(pos1.getZ(), pos2.getZ());
        double z2 = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getY() >= y1 && loc.getY() <= y2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }

    // NEW: ignore Y completely (X/Z only)
    public boolean isInsideIgnoreY(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        double x1 = Math.min(pos1.getX(), pos2.getX());
        double x2 = Math.max(pos1.getX(), pos2.getX());
        double z1 = Math.min(pos1.getZ(), pos2.getZ());
        double z2 = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }



    public Location getSpawn() {
        return spawn;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }
}
