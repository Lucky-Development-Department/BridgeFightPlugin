package me.molfordan.arenaAndFFAManager.config;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BridgeFightConfig {

    private final ArenaAndFFAManager plugin;
    private File file;
    private FileConfiguration config;

    public BridgeFightConfig(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bridgefight.yml");
    }

    /**
     * Load or create bridgefight.yml
     */
    public void load() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // If missing, try copying from resources first
        if (!file.exists()) {
            if (plugin.getResource("bridgefight.yml") != null) {
                plugin.saveResource("bridgefight.yml", false);
            } else {
                try { file.createNewFile(); }
                catch (IOException e) { e.printStackTrace(); }
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        if (config == null) load();
        return config;
    }

    public void save() {
        if (config == null || file == null) return;

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}
