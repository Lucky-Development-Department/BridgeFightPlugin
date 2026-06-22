package me.molfordan.bridgefightplugin.kits.bridgefightkit;

import me.molfordan.bridgefightplugin.BridgeFightPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwordChoiceManager {

    private final BridgeFightPlugin plugin;
    private final File file;
    private final FileConfiguration config;
    
    private final Map<UUID, Material> selectedSwords = new HashMap<>();
    private final Map<UUID, Boolean> sharpnessOptions = new HashMap<>();
    private final Map<UUID, String> selectedArmorKits = new HashMap<>();

    public SwordChoiceManager(BridgeFightPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sword_choices.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public Material getSelectedSword(UUID uuid) {
        if (selectedSwords.containsKey(uuid)) {
            return selectedSwords.get(uuid);
        }
        String matName = config.getString(uuid.toString() + ".sword", "WOOD_SWORD");
        Material mat;
        try {
            mat = Material.valueOf(matName);
        } catch (Exception e) {
            mat = Material.WOOD_SWORD;
        }
        selectedSwords.put(uuid, mat);
        return mat;
    }

    public void setSelectedSword(UUID uuid, Material material) {
        selectedSwords.put(uuid, material);
        config.set(uuid.toString() + ".sword", material.name());
        save();
    }

    public boolean hasSharpness(UUID uuid) {
        if (sharpnessOptions.containsKey(uuid)) {
            return sharpnessOptions.get(uuid);
        }
        boolean sharp = config.getBoolean(uuid.toString() + ".sharpness", false);
        sharpnessOptions.put(uuid, sharp);
        return sharp;
    }

    public void setSharpness(UUID uuid, boolean sharp) {
        sharpnessOptions.put(uuid, sharp);
        config.set(uuid.toString() + ".sharpness", sharp);
        save();
    }

    public String getSelectedArmorKit(UUID uuid) {
        if (selectedArmorKits.containsKey(uuid)) {
            return selectedArmorKits.get(uuid);
        }
        String kit = config.getString(uuid.toString() + ".armor_kit", "Default");
        selectedArmorKits.put(uuid, kit);
        return kit;
    }

    public void setSelectedArmorKit(UUID uuid, String kitName) {
        selectedArmorKits.put(uuid, kitName);
        config.set(uuid.toString() + ".armor_kit", kitName);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
