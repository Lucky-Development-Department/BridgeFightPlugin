package me.molfordan.bridgefightplugin.config;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class KnockbackConfig {

    private final BridgeFightPlugin plugin;
    private File file;
    private YamlConfiguration config;

    public KnockbackConfig(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        file = new File(plugin.getDataFolder(), "knockback.yml");
        if (!file.exists()) {
            plugin.saveResource("knockback.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
