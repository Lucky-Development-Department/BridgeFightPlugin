package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.object.Arena;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.object.SerializableBlockState;
import me.molfordan.arenaAndFFAManager.listener.LadderRestorer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {
    private final File dataFolder;
    private final ArenaAndFFAManager plugin;
    private final LadderRestorer ladderRestorer;
    private final Map<String, Arena> arenaMap = new HashMap<>();
    private final Set<UUID> bypassingPlayers = new HashSet<>();

    public ArenaManager(File dataFolder, ArenaAndFFAManager plugin, LadderRestorer ladderRestorer) {
        this.dataFolder = dataFolder;
        this.plugin = plugin;
        this.ladderRestorer = ladderRestorer;
    }

    public Arena createArena(String name, org.bukkit.entity.Player creator) {
        if (arenaMap.containsKey(name)) return null;
        Arena arena = new Arena(name, creator.getWorld());
        arena.setWorldName(creator.getWorld().getName());
        arenaMap.put(name, arena);
        saveArena(arena);
        return arena;
    }

    public boolean removeArena(String name) {
        Arena removed = arenaMap.remove(name);
        if (removed == null) return false;
        File file = new File(dataFolder, "arenas.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("arenas." + name, null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Collection<Arena> getAllArenas() {
        return arenaMap.values();
    }

    public Arena getArenaByName(String name) {
        return arenaMap.get(name);
    }

    public Arena getArenaByLocation(Location loc) {
        for (Arena arena : arenaMap.values()) {
            if (!arena.getWorldName().equals(loc.getWorld().getName())) continue;
            if (arena.getPos1() == null || arena.getPos2() == null) continue;

            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            int minX = Math.min(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int maxX = Math.max(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int minY = Math.min(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
            int maxY = Math.max(arena.getPos1().getBlockY(), arena.getPos2().getBlockY());
            int minZ = Math.min(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());
            int maxZ = Math.max(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());

            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                return arena;
            }
        }
        return null;
    }

    public Arena getArenaByLocationIgnoreY(Location loc) {
        for (Arena arena : arenaMap.values()) {
            if (!arena.getWorldName().equals(loc.getWorld().getName())) continue;
            if (arena.getPos1() == null || arena.getPos2() == null) continue;

            int x = loc.getBlockX();
            int z = loc.getBlockZ();

            int minX = Math.min(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int maxX = Math.max(arena.getPos1().getBlockX(), arena.getPos2().getBlockX());
            int minZ = Math.min(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());
            int maxZ = Math.max(arena.getPos1().getBlockZ(), arena.getPos2().getBlockZ());

            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return arena;
            }
        }
        return null;
    }

    public void saveAllArenas() {
        for (Arena arena : arenaMap.values()) {
            saveArena(arena);
        }
    }

    public void saveArena(Arena arena) {
        File file = new File(dataFolder, "arenas.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "arenas." + arena.getName();
        config.set(path, null); // clear old data

        config.set(path + ".world", arena.getWorldName());
        config.set(path + ".type", arena.getType() != null ? arena.getType().name() : null);
        config.set(path + ".voidLimit", arena.getVoidLimit());
        config.set(path + ".buildLimit", arena.getBuildLimitY());
        config.set(path + ".finished", arena.isFinished());

        saveLocation(config, path + ".center", arena.getCenter());
        saveLocation(config, path + ".pos1", arena.getPos1());
        saveLocation(config, path + ".pos2", arena.getPos2());

        if (arena.getType() == ArenaType.FFABUILD && arena.getOriginalBlocksMap() != null) {
            for (Map.Entry<String, SerializableBlockState> entry : arena.getOriginalBlocksMap().entrySet()) {
                String key = path + ".blocks." + entry.getKey();
                config.set(key + ".type", entry.getValue().getType().name());
                config.set(key + ".data", entry.getValue().getData());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }










    public void loadArenas() {
        File file = new File(dataFolder, "arenas.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");

        if (arenasSection == null) return;

        for (String name : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(name);
            if (section == null) continue;

            try {
                String worldName = section.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Arena arena = new Arena(name, world);
                arena.setWorldName(worldName);
                arena.setType(ArenaType.fromString(section.getString("type")));
                arena.setVoidLimit(section.getInt("voidLimit"));
                arena.setBuildLimitY(section.getInt("buildLimit"));
                arena.setFinished(section.getBoolean("finished"));
                arena.setCenter(getLocation(section.getConfigurationSection("center")));
                arena.setPos1(getLocation(section.getConfigurationSection("pos1")));
                arena.setPos2(getLocation(section.getConfigurationSection("pos2")));



                ConfigurationSection blockSection = section.getConfigurationSection("blocks");
                if (blockSection != null) {
                    for (String key : blockSection.getKeys(false)) {
                        ConfigurationSection b = blockSection.getConfigurationSection(key);
                        if (b == null) continue;
                        Material mat = Material.matchMaterial(b.getString("type"));
                        byte data = (byte) b.getInt("data");
                        if (mat != null) {
                            arena.getOriginalBlocksMap().put(key, new SerializableBlockState(mat, data));
                        }
                    }
                }

                arenaMap.put(name, arena);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to load arena: " + name);
                e.printStackTrace();
            }
        }
    }

    private void saveLocation(YamlConfiguration config, String path, Location loc) {
        if (loc == null) return;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location getLocation(ConfigurationSection section) {
        if (section == null) return null;
        try {
            // Try native Bukkit deserialization first
            Object loc = section.get(".");
            if (loc instanceof Location) {
                Location l = (Location) loc;
                if (l.getWorld() == null && section.isString("world")) {
                    World world = Bukkit.getWorld(section.getString("world"));
                    if (world != null) l.setWorld(world);
                }
                return l;
            }

            // Manual fallback
            String worldName = section.getString("world");
            World world = Bukkit.getWorld(worldName);
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw", 0);
            float pitch = (float) section.getDouble("pitch", 0);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to deserialize location section: " + section.getName());
            return null;
        }
    }





    public Arena cloneArena(Arena source, String newName, World newWorld) {
        if (arenaMap.containsKey(newName)) return null;

        Arena clone = new Arena(newName, newWorld);
        clone.setWorldName(newWorld.getName());
        clone.setType(source.getType());
        clone.setVoidLimit(source.getVoidLimit());
        clone.setBuildLimitY(source.getBuildLimitY());
        clone.setFinished(source.isFinished());

        if (source.getCenter() != null)
            clone.setCenter(source.getCenter().clone());
        if (source.getPos1() != null)
            clone.setPos1(source.getPos1().clone());
        if (source.getPos2() != null)
            clone.setPos2(source.getPos2().clone());

        if (source.getOriginalBlocksMap() != null) {
            for (Map.Entry<String, SerializableBlockState> entry : source.getOriginalBlocksMap().entrySet()) {
                clone.getOriginalBlocksMap().put(entry.getKey(), entry.getValue());
            }
        }

        arenaMap.put(newName, clone);
        return clone;
    }

    public boolean isInArena(Player player) {
        return getArenaByLocation(player.getLocation()) != null;
    }

    public boolean isInArenaIgnoreY(Player player) {
        return getArenaByLocationIgnoreY(player.getLocation()) != null;
    }

    public boolean isBypassing(UUID uuid) {
        return bypassingPlayers.contains(uuid);
    }

    public void toggleBypass(UUID uuid) {
        if (bypassingPlayers.contains(uuid)) {
            bypassingPlayers.remove(uuid);
        } else {
            bypassingPlayers.add(uuid);
        }
    }
}
