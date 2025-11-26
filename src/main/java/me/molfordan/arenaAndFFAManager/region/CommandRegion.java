package me.molfordan.arenaAndFFAManager.region;

import org.bukkit.Location;

import java.util.EnumMap;
import java.util.Map;

public final class CommandRegion {

    public enum Executor { CONSOLE, PLAYER, NULL}

    private Location pos1;
    private Location pos2;

    private String command;
    private Executor executor;

    private final Map<FlagType, String> flags = new EnumMap<>(FlagType.class);

    private int minX, maxX, minY, maxY, minZ, maxZ;

    public CommandRegion(Location pos1, Location pos2, String command, Executor executor) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.command = command == null ? "" : command;
        this.executor = executor == null ? Executor.CONSOLE : executor;
        recalcBounds();
    }

    private void recalcBounds() {
        if (pos1 == null || pos2 == null) return;

        minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());

        minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());

        minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public boolean isInside(Location loc) {
        if (loc == null || pos1 == null || pos2 == null) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    public void setPos1(Location pos1) { this.pos1 = pos1; recalcBounds(); }
    public void setPos2(Location pos2) { this.pos2 = pos2; recalcBounds(); }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }

    public String getCommand() { return command; }
    public Executor getExecutor() { return executor; }

    // NEW
    public void setCommand(String command) {
        this.command = command == null ? "" : command;
    }

    // NEW
    public void setExecutor(Executor executor) {
        if (executor != null) this.executor = executor;
    }

    public void setFlag(FlagType type, String value) {
        flags.put(type, value.toLowerCase());
    }

    public String getFlag(FlagType type) {
        return flags.get(type);
    }

    public Map<FlagType, String> getFlags() {
        return flags;
    }

    public boolean isBuildDenied() {
        String val = flags.get(FlagType.BUILD);
        if (val == null) return false;
        return val.equalsIgnoreCase("deny") || val.equalsIgnoreCase("false");
    }
    public boolean isFlagAllowed(FlagType type) {
        String val = flags.get(type);
        return val != null && val.equalsIgnoreCase("allow");
    }

    public boolean isFlagDenied(FlagType type) {
        String val = flags.get(type);
        return val != null && val.equalsIgnoreCase("deny");
    }

}
