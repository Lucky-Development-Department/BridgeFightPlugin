package me.molfordan.bridgefightplugin.cosmetics;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import me.molfordan.bridgefightplugin.cosmetics.objects.CosmeticTier;
import me.molfordan.bridgefightplugin.cosmetics.objects.KillEffect;
import me.molfordan.bridgefightplugin.cosmetics.objects.KillMessage;
import me.molfordan.bridgefightplugin.cosmetics.objects.Trail;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class CosmeticsManager {

    private final BridgeFightPlugin plugin;
    private FileConfiguration config;
    private final File configFile;

    private final Map<String, KillMessage> killMessages = new LinkedHashMap<>();
    private final Map<String, KillEffect> killEffects = new LinkedHashMap<>();
    private final Map<String, Trail> trails = new LinkedHashMap<>();

    public CosmeticsManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("cosmetics.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = plugin.getResource("cosmetics.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            config.setDefaults(defaultConfig);
        }

        loadKillMessages();
        loadKillEffects();
        loadTrails();
    }

    private void loadKillMessages() {
        killMessages.clear();
        ConfigurationSection section = config.getConfigurationSection("kill_messages");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display_name", key);
            String message = section.getString(key + ".message", "");
            String voidMessage = section.getString(key + ".void_message", message);
            int requiredBalance = section.getInt(key + ".required_balance", 0);
            String permission = section.getString(key + ".permission", "");
            String tierStr = section.getString(key + ".tier", "COMMON");
            CosmeticTier tier = CosmeticTier.fromString(tierStr);
            killMessages.put(key, new KillMessage(key, displayName, message, voidMessage, requiredBalance, permission, tier));
        }
    }

    private void loadKillEffects() {
        killEffects.clear();
        ConfigurationSection section = config.getConfigurationSection("kill_effects");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display_name", key);
            String effectType = section.getString(key + ".effect_type", "NONE");
            int requiredBalance = section.getInt(key + ".required_balance", 0);
            String permission = section.getString(key + ".permission", "");
            String tierStr = section.getString(key + ".tier", "COMMON");
            CosmeticTier tier = CosmeticTier.fromString(tierStr);
            killEffects.put(key, new KillEffect(key, displayName, effectType, requiredBalance, permission, tier));
        }
    }

    private void loadTrails() {
        trails.clear();
        ConfigurationSection section = config.getConfigurationSection("trails");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display_name", key);
            String particle = section.getString(key + ".particle", "NONE");
            int requiredBalance = section.getInt(key + ".required_balance", 0);
            String permission = section.getString(key + ".permission", "");
            String tierStr = section.getString(key + ".tier", "COMMON");
            CosmeticTier tier = CosmeticTier.fromString(tierStr);
            trails.put(key, new Trail(key, displayName, particle, requiredBalance, permission, tier));
        }
    }

    public Map<String, KillMessage> getKillMessages() { return killMessages; }
    public Map<String, KillEffect> getKillEffects() { return killEffects; }
    public Map<String, Trail> getTrails() { return trails; }

    public KillMessage getKillMessage(String id) { 
        return killMessages.getOrDefault(id, killMessages.get("default")); 
    }
    
    public KillEffect getKillEffect(String id) { 
        return killEffects.getOrDefault(id, killEffects.get("none")); 
    }
    
    public Trail getTrail(String id) { 
        return trails.getOrDefault(id, trails.get("none")); 
    }
}
