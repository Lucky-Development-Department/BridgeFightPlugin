package me.molfordan.arenaAndFFAManager.config;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class KnockbackConfig {

    private final ArenaAndFFAManager plugin;
    private File file;
    private YamlConfiguration config;

    public KnockbackConfig(ArenaAndFFAManager plugin) {
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
