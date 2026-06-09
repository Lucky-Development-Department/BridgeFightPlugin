package me.molfordan.bridgefightplugin.config;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RegionsConfig {

    private final BridgeFightPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public RegionsConfig(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "Regions.yml");
        ensureFile();
        load();
    }

    private void ensureFile() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        if (config == null) load();
        return config;
    }

    public void save() {
        try {
            getConfig().save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        load();
    }
}
