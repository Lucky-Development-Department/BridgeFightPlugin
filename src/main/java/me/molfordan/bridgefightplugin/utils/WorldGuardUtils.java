package me.molfordan.bridgefightplugin.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class WorldGuardUtils {

    private static boolean available = false;
    private static boolean isVersion7 = false;

    public static void initialize(Plugin plugin) {
        Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
        available = wg != null && wg.isEnabled();

        if (available) {
            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                isVersion7 = true;
            } catch (ClassNotFoundException e) {
                isVersion7 = false;
            }
        }
    }

    public static boolean isWorldGuardAvailable() {
        return available;
    }

    public static boolean isInAnyRegion(Location location) {
        if (!available || location == null) return false;

        try {
            if (isVersion7) {
                return WorldGuardV7Handler.isInAnyRegion(location);
            } else {
                return WorldGuardV6Handler.isInAnyRegion(location);
            }
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static Set<String> getRegionNamesAt(Location location) {
        if (!available || location == null) return new HashSet<>();

        try {
            if (isVersion7) {
                return WorldGuardV7Handler.getRegionNamesAt(location);
            } else {
                return WorldGuardV6Handler.getRegionNamesAt(location);
            }
        } catch (NoClassDefFoundError e) {
            return new HashSet<>();
        }
    }

    /**
     * Inner class to isolate WorldGuard 7.x dependencies.
     * The JVM will only load this class if WorldGuard 7 is detected.
     */
    private static class WorldGuardV7Handler {
        private static boolean isInAnyRegion(Location loc) {
            try {
                com.sk89q.worldguard.protection.managers.RegionManager manager = 
                    com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(loc.getWorld()));
                if (manager == null) return false;
                return manager.getApplicableRegions(com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockVector(loc)).size() > 0;
            } catch (Exception e) {
                return false;
            }
        }

        private static Set<String> getRegionNamesAt(Location loc) {
            Set<String> names = new HashSet<>();
            try {
                com.sk89q.worldguard.protection.managers.RegionManager manager = 
                    com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(loc.getWorld()));
                if (manager != null) {
                    for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : 
                         manager.getApplicableRegions(com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockVector(loc))) {
                        names.add(region.getId());
                    }
                }
            } catch (Exception e) {}
            return names;
        }
    }

    /**
     * Inner class to handle WorldGuard 6.x using reflection.
     * This avoids compile-time dependencies on WG6 classes while the POM is set to WG7.
     */
    private static class WorldGuardV6Handler {
        private static boolean isInAnyRegion(Location loc) {
            try {
                Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
                Method getRegionManagerMethod = wg.getClass().getMethod("getRegionManager", World.class);
                Object manager = getRegionManagerMethod.invoke(wg, loc.getWorld());
                if (manager == null) return false;

                Method getApplicableRegionsMethod = manager.getClass().getMethod("getApplicableRegions", Location.class);
                Object set = getApplicableRegionsMethod.invoke(manager, loc);
                Method sizeMethod = set.getClass().getMethod("size");
                return (int) sizeMethod.invoke(set) > 0;
            } catch (Exception e) {
                return false;
            }
        }

        private static Set<String> getRegionNamesAt(Location loc) {
            Set<String> names = new HashSet<>();
            try {
                Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
                Method getRegionManagerMethod = wg.getClass().getMethod("getRegionManager", World.class);
                Object manager = getRegionManagerMethod.invoke(wg, loc.getWorld());
                if (manager != null) {
                    Method getApplicableRegionsMethod = manager.getClass().getMethod("getApplicableRegions", Location.class);
                    Object set = getApplicableRegionsMethod.invoke(manager, loc);
                    if (set instanceof Iterable) {
                        for (Object region : (Iterable<?>) set) {
                            Method getIdMethod = region.getClass().getMethod("getId");
                            names.add((String) getIdMethod.invoke(region));
                        }
                    }
                }
            } catch (Exception e) {}
            return names;
        }
    }
}
