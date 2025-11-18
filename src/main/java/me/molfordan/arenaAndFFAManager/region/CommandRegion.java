package me.molfordan.arenaAndFFAManager.region;

import org.bukkit.Location;

import java.util.Objects;

public final class CommandRegion {

    public enum Executor { CONSOLE, PLAYER }

    private Location pos1;
    private Location pos2;
    private final String command;
    private final Executor executor;

    // cached block-aligned bounds
    private int minX, maxX, minY, maxY, minZ, maxZ;

    public CommandRegion(Location pos1, Location pos2, String command, Executor executor) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.command = command == null ? "" : command;
        this.executor = executor == null ? Executor.CONSOLE : executor;

        recalcBounds();
    }

    /** Convert raw pos1/pos2 into FULL BLOCK cuboid bounds */
    private void recalcBounds() {
        if (pos1 == null || pos2 == null) return;

        minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());

        minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());

        minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // expand cuboid by 1 block so it's inclusive and enterable
        maxX++;
        maxY++;
        maxZ++;
    }

    /** Player enters if their location block coords fall inside cuboid */
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

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public String getCommand() { return command; }
    public Executor getExecutor() { return executor; }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
        recalcBounds();
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
        recalcBounds();
    }

    @Override
    public String toString() {
        return "CommandRegion{" +
                "pos1=" + pos1 +
                ", pos2=" + pos2 +
                ", command='" + command + '\'' +
                ", executor=" + executor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CommandRegion)) return false;
        CommandRegion other = (CommandRegion) o;
        return Objects.equals(pos1, other.pos1) &&
                Objects.equals(pos2, other.pos2) &&
                Objects.equals(command, other.command) &&
                executor == other.executor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos1, pos2, command, executor);
    }
}
