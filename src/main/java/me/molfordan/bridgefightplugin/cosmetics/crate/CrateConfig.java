package me.molfordan.bridgefightplugin.cosmetics.crate;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CrateConfig {
    private final BridgeFightPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public CrateConfig(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create crates.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public List<Location> getCrateLocations() {
        List<Location> locations = new ArrayList<>();
        List<String> list = config.getStringList("crates");
        if (list == null) return locations;

        for (String entry : list) {
            String[] parts = entry.split(",");
            if (parts.length >= 4) {
                try {
                    String worldName = parts[0];
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    if (Bukkit.getWorld(worldName) != null) {
                        locations.add(new Location(Bukkit.getWorld(worldName), x, y, z));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return locations;
    }

    public void saveCrateLocations(List<Location> locations) {
        List<String> list = new ArrayList<>();
        for (Location loc : locations) {
            if (loc.getWorld() != null) {
                list.add(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        }
        config.set("crates", list);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crates.yml: " + e.getMessage());
        }
    }
}
