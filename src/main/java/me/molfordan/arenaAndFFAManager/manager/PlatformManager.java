package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.object.PlatformRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlatformManager {

    private final Map<String, PlatformRegion> platforms = new HashMap<>();

    /**
     * Create or get platform (used only internally)
     */
    public PlatformRegion getPlatform(String name) {
        return platforms.computeIfAbsent(name, n -> new PlatformRegion(null, null, null));
    }

    public Map<String, PlatformRegion> getAllPlatforms() {
        return platforms;
    }

    /**
     * Get platform ONLY if already exists
     */
    public PlatformRegion getExistingPlatform(String name) {
        return platforms.get(name);
    }

    /**
     * XYZ full cuboid check
     */
    public PlatformRegion fromLocation(Location loc) {
        if (loc == null) return null;

        for (PlatformRegion region : platforms.values()) {
            if (region.isComplete() && region.isInside(loc)) {
                return region;
            }
        }
        return null;
    }

    /**
     * Load platforms from config (robust)
     */
    public void loadFromConfig(FileConfiguration config) {

        if (!config.isConfigurationSection("platforms")) {
            System.out.println("[BridgeFight] No platforms found in config.");
            return;
        }

        for (String name : config.getConfigurationSection("platforms").getKeys(false)) {

            PlatformRegion region = getPlatform(name);

            // Load spawn
            Location spawn = readLocation(config, "platforms." + name + ".spawn");
            if (spawn != null) region.setSpawn(spawn);

            // Load pos1
            Location pos1 = readLocation(config, "platforms." + name + ".pos1");
            if (pos1 != null) region.setPos1(pos1);

            // Load pos2
            Location pos2 = readLocation(config, "platforms." + name + ".pos2");
            if (pos2 != null) region.setPos2(pos2);

            System.out.println("[BridgeFight] Loaded platform: " + name);
        }
    }

    /**
     * SAFE LOCATION LOADER
     */
    private Location readLocation(FileConfiguration cfg, String path) {

        if (!cfg.isConfigurationSection(path)) return null;

        String worldName = cfg.getString(path + ".world");
        if (worldName == null) {
            System.out.println("[BridgeFight] Missing world in config at: " + path);
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            System.out.println("[BridgeFight] World not loaded: " + worldName + " (path: " + path + ")");
            return null; // DO NOT RETURN A LOCATION WITH null WORLD
        }

        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Get ALL platforms
     */
    public Collection<PlatformRegion> getAll() {
        return platforms.values();
    }

    /**
     * XZ-only check (ignore Y)
     */
    public PlatformRegion fromLocationIgnoreY(Location loc) {
        if (loc == null) return null;

        double x = loc.getX();
        double z = loc.getZ();

        for (PlatformRegion region : platforms.values()) {

            if (region == null || region.getPos1() == null || region.getPos2() == null)
                continue;

            double minX = Math.min(region.getPos1().getX(), region.getPos2().getX());
            double maxX = Math.max(region.getPos1().getX(), region.getPos2().getX());

            double minZ = Math.min(region.getPos1().getZ(), region.getPos2().getZ());
            double maxZ = Math.max(region.getPos1().getZ(), region.getPos2().getZ());

            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ)
                return region;
        }

        return null;
    }
}
