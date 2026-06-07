package me.molfordan.arenaAndFFAManager.bedfight;

import com.grinderwolf.swm.api.world.SlimeWorld;
import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedFightArenaManager {
    private final ArenaAndFFAManager plugin;
    private final File bedFightFolder;
    private final Map<String, Arena> bedFightArenas = new HashMap<>();
    private final Map<String, SlimeWorld> slimeTemplates = new HashMap<>();
    private final Map<UUID, BedFightSetupSession> setupSessions = new HashMap<>();
    private SlimeWorldAdapter slimeAdapter;

    public BedFightArenaManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.bedFightFolder = new File(plugin.getDataFolder(), "bedfight");
        if (!bedFightFolder.exists()) {
            bedFightFolder.mkdirs();
        }
        
        // Initialize adapter in a task to ensure SWM is loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
                this.slimeAdapter = new SlimeWorldAdapter();
                loadArenas();
            } else {
                plugin.getLogger().severe("SlimeWorldManager not found! BedFight will not work correctly.");
            }
        }, 1L);
    }

    public void loadArenas() {
        if (slimeAdapter == null) return;
        
        bedFightArenas.clear();
        slimeTemplates.clear();
        
        File[] files = bedFightFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = config.getConfigurationSection("arena");
            if (section == null) continue;

            String name = section.getString("name");
            String worldName = section.getString("world");
            
            Arena arena = new Arena(name, Bukkit.getWorlds().get(0));
            arena.setWorldName(worldName);
            loadArenaData(arena, section);
            
            bedFightArenas.put(name.toLowerCase(), arena);
            
            // Load Slime template
            try {
                SlimeWorld template = slimeAdapter.loadTemplate(name);
                slimeTemplates.put(name.toLowerCase(), template);
                plugin.getLogger().info("Loaded BedFight arena & Slime template: " + name);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load Slime template for " + name + " (Make sure the .slime file exists in SWM imports/folder): " + e.getMessage());
            }
        }
    }

    private void loadArenaData(Arena arena, ConfigurationSection section) {
        arena.setType(me.molfordan.arenaAndFFAManager.object.enums.ArenaType.BEDFIGHT);
        arena.setVoidLimit(section.getInt("voidLimit"));
        arena.setBuildLimitY(section.getInt("buildLimit"));
        arena.setFinished(section.getBoolean("finished"));
        
        arena.setPos1(getLocation(section.getConfigurationSection("pos1")));
        arena.setPos2(getLocation(section.getConfigurationSection("pos2")));
        arena.setCenter(getLocation(section.getConfigurationSection("center")));
        arena.setRedSpawn(getLocation(section.getConfigurationSection("redSpawn")));
        arena.setBlueSpawn(getLocation(section.getConfigurationSection("blueSpawn")));
        arena.setRedBed(getLocation(section.getConfigurationSection("redBed")));
        arena.setBlueBed(getLocation(section.getConfigurationSection("blueBed")));
    }

    private Location getLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Could not find world with name: " + worldName);
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void saveArena(Arena arena) {
        File file = new File(bedFightFolder, arena.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        String path = "arena";

        config.set(path + ".name", arena.getName());
        config.set(path + ".world", arena.getWorldName());
        config.set(path + ".type", arena.getType().name());
        config.set(path + ".voidLimit", arena.getVoidLimit());
        config.set(path + ".buildLimit", arena.getBuildLimitY());
        config.set(path + ".finished", arena.isFinished());

        saveLocation(config, path + ".pos1", arena.getPos1());
        saveLocation(config, path + ".pos2", arena.getPos2());
        saveLocation(config, path + ".center", arena.getCenter());
        saveLocation(config, path + ".redSpawn", arena.getRedSpawn());
        saveLocation(config, path + ".blueSpawn", arena.getBlueSpawn());
        saveLocation(config, path + ".redBed", arena.getRedBed());
        saveLocation(config, path + ".blueBed", arena.getBlueBed());

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLocation(YamlConfiguration config, String path, Location loc) {
        if (loc == null) return;
        config.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    public Arena getArena(String name) {
        return bedFightArenas.get(name.toLowerCase());
    }

    public SlimeWorld getSlimeTemplate(String name) {
        return slimeTemplates.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return bedFightArenas.values();
    }

    public SlimeWorldAdapter getSlimeAdapter() {
        return slimeAdapter;
    }

    public void startSetupSession(Player player, String arenaName, Arena arena) {
        setupSessions.put(player.getUniqueId(), new BedFightSetupSession(player.getUniqueId(), arenaName, arena));
    }

    public BedFightSetupSession getSetupSession(Player player) {
        return setupSessions.get(player.getUniqueId());
    }

    public void removeSetupSession(Player player) {
        setupSessions.remove(player.getUniqueId());
    }
}
