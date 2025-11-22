package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;

public class ConfigManager {
    private final ArenaAndFFAManager plugin;
    private FileConfiguration config;
    private File configFile;
    private static final String LOBBY_PATH = "lobby";
    private static final String BUILDFFA_PATH = "buildffa";
    private static final String BRIDGEFIGHT_PATH = "bridgefight";
    private static final String PRIVATEWORLD_PATH = "privateworld";
    private final Set<UUID> buildMode = new HashSet<>();

    public ConfigManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        setupConfig();
        loadLocations();
        ensureConfigDefaults();
        plugin.saveDefaultConfig();
    }

    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            plugin.debug("Created new config.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults();
    }

    private void setDefaults() {
        config.options().copyDefaults(true);
        saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.debug("Could not save config.yml!");
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.debug("Config reloaded!");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void setPrivateWorldLocation(Location loc){
        config.set(PRIVATEWORLD_PATH + ".world", loc.getWorld().getName());
        config.set(PRIVATEWORLD_PATH + ".x", loc.getX());
        config.set(PRIVATEWORLD_PATH + ".y", loc.getY());
        config.set(PRIVATEWORLD_PATH + ".z", loc.getZ());
        config.set(PRIVATEWORLD_PATH + ".yaw", loc.getYaw());
        config.set(PRIVATEWORLD_PATH + ".pitch", loc.getPitch());
        saveConfig();
    }

    public void setLobbyLocation(Location loc) {
        // Paths will now be "lobby.world", "lobby.x", etc.
        config.set(LOBBY_PATH + ".world", loc.getWorld().getName());
        config.set(LOBBY_PATH + ".x", loc.getX());
        config.set(LOBBY_PATH + ".y", loc.getY());
        config.set(LOBBY_PATH + ".z", loc.getZ());
        config.set(LOBBY_PATH + ".yaw", loc.getYaw());
        config.set(LOBBY_PATH + ".pitch", loc.getPitch());
        saveConfig();
    }

    public void setBuildFFALocation(Location loc){
        config.set(BUILDFFA_PATH + ".world", loc.getWorld().getName());
        config.set(BUILDFFA_PATH + ".x", loc.getX());
        config.set(BUILDFFA_PATH + ".y", loc.getY());
        config.set(BUILDFFA_PATH + ".z", loc.getZ());
        config.set(BUILDFFA_PATH + ".yaw", loc.getYaw());
        config.set(BUILDFFA_PATH + ".pitch", loc.getPitch());
        saveConfig();
    }

    public void setBridgeFightLocation(Location loc){
        config.set(BRIDGEFIGHT_PATH + ".world", loc.getWorld().getName());
        config.set(BRIDGEFIGHT_PATH + ".x", loc.getX());
        config.set(BRIDGEFIGHT_PATH + ".y", loc.getY());
        config.set(BRIDGEFIGHT_PATH + ".z", loc.getZ());
        config.set(BRIDGEFIGHT_PATH + ".yaw", loc.getYaw());
        config.set(BRIDGEFIGHT_PATH + ".pitch", loc.getPitch());

        saveConfig();
    }

    public void loadLobbyLocations(){
        if (!config.contains(LOBBY_PATH)) {
            config.set(LOBBY_PATH + ".world", "Lobby");
            config.set(LOBBY_PATH + ".x", null);
            config.set(LOBBY_PATH + ".y", null);
            config.set(LOBBY_PATH + ".z", null);
            config.set(LOBBY_PATH + ".yaw", null);
            config.set(LOBBY_PATH + ".pitch", null);
        }

        saveConfig();
    }

    private void loadPrivateWorldLocations(){
        if (!config.contains(PRIVATEWORLD_PATH)) {
            config.set(PRIVATEWORLD_PATH + ".world", "PrivateWorld");
            config.set(PRIVATEWORLD_PATH + ".x", null);
            config.set(PRIVATEWORLD_PATH + ".y", null);
            config.set(PRIVATEWORLD_PATH + ".z", null);
            config.set(PRIVATEWORLD_PATH + ".yaw", null);
            config.set(PRIVATEWORLD_PATH + ".pitch", null);
        }

        saveConfig();
    }

    public void loadBuildFFALocations(){
        if (!config.contains(BUILDFFA_PATH)) {
            config.set(BUILDFFA_PATH + ".world", "BuildFFA");
            config.set(BUILDFFA_PATH + ".x", null);
            config.set(BUILDFFA_PATH + ".y", null);
            config.set(BUILDFFA_PATH + ".z", null);
            config.set(BUILDFFA_PATH + ".yaw", null);
            config.set(BUILDFFA_PATH + ".pitch", null);
        }

        saveConfig();
    }

    public void loadBridgeFightLocations(){
        if (!config.contains(BRIDGEFIGHT_PATH)) {
            config.set(BRIDGEFIGHT_PATH + ".world", "BridgeFight");
            config.set(BRIDGEFIGHT_PATH + ".x", null);
            config.set(BRIDGEFIGHT_PATH + ".y", null);
            config.set(BRIDGEFIGHT_PATH + ".z", null);
            config.set(BRIDGEFIGHT_PATH + ".yaw", null);
            config.set(BRIDGEFIGHT_PATH + ".pitch", null);
        }

        saveConfig();
    }

    public void loadLocations(){
        loadLobbyLocations();
        loadBridgeFightLocations();
        loadBuildFFALocations();
        loadPrivateWorldLocations();
    }

    public Location getLobbyLocation() {
        if (!config.contains(LOBBY_PATH + ".world")) {
            return null; // Location is not set
        }

        String worldName = config.getString(LOBBY_PATH + ".world");
        // Get the World object (crucial for location creation)
        org.bukkit.World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            // World might be unloaded or deleted
            return null;
        }

        double x = config.getDouble(LOBBY_PATH + ".x");
        double y = config.getDouble(LOBBY_PATH + ".y");
        double z = config.getDouble(LOBBY_PATH + ".z");
        float yaw = (float) config.getDouble(LOBBY_PATH + ".yaw");
        float pitch = (float) config.getDouble(LOBBY_PATH + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getPrivateWorldLocation(){
        if (!config.contains(PRIVATEWORLD_PATH + ".world")) {
            return null; // Location is not set
        }

        String worldName = config.getString(PRIVATEWORLD_PATH + ".world");
        // Get the World object (crucial for location creation)
        org.bukkit.World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            // World might be unloaded or deleted
            return null;
        }

        double x = config.getDouble(PRIVATEWORLD_PATH + ".x");
        double y = config.getDouble(PRIVATEWORLD_PATH + ".y");
        double z = config.getDouble(PRIVATEWORLD_PATH + ".z");
        float yaw = (float) config.getDouble(PRIVATEWORLD_PATH + ".yaw");
        float pitch = (float) config.getDouble(PRIVATEWORLD_PATH + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getLobbyWorldName(){
        return config.getString(LOBBY_PATH + ".world");
    }

    public String getPrivateWorldWorldName(){
        return config.getString(PRIVATEWORLD_PATH + ".world");
    }

    public String getBuildFFAWorldName(){
        return config.getString(BUILDFFA_PATH + ".world");
    }

    public String getBridgeFightWorldName(){
        return config.getString(BRIDGEFIGHT_PATH + ".world");
    }

    public Location getBuildFFALocation(){
        if (!config.contains(BUILDFFA_PATH + ".world")) {
            return null; // Location is not set
        }

        String worldName = config.getString(BUILDFFA_PATH + ".world");
        // Get the World object (crucial for location creation)
        org.bukkit.World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            // World might be unloaded or deleted
            return null;
        }

        double x = config.getDouble(BUILDFFA_PATH + ".x");
        double y = config.getDouble(BUILDFFA_PATH + ".y");
        double z = config.getDouble(BUILDFFA_PATH + ".z");
        float yaw = (float) config.getDouble(BUILDFFA_PATH + ".yaw");
        float pitch = (float) config.getDouble(BUILDFFA_PATH + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getBridgeFightLocation(){
        if (!config.contains(BRIDGEFIGHT_PATH + ".world")) {
            return null; // Location is not set
        }

        String worldName = config.getString(BRIDGEFIGHT_PATH + ".world");
        // Get the World object (crucial for location creation)
        org.bukkit.World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            // World might be unloaded or deleted
            return null;
        }

        double x = config.getDouble(BRIDGEFIGHT_PATH + ".x");
        double y = config.getDouble(BRIDGEFIGHT_PATH + ".y");
        double z = config.getDouble(BRIDGEFIGHT_PATH + ".z");
        float yaw = (float) config.getDouble(BRIDGEFIGHT_PATH + ".yaw");
        float pitch = (float) config.getDouble(BRIDGEFIGHT_PATH + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void ensureConfigDefaults(){
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;
        if (!config.contains("debug")){
            config.set("debug", false);
            changed = true;
        }
        if (!config.contains("BridgeFightVoidLimit")){
            config.set("BridgeFightVoidLimit", 80.0);
            changed = true;
        }
        if (!config.contains("ServerPrefix")){
            config.set("ServerPrefix", "&e&lBRIDGEFIGHT &a/");
            changed = true;
        }
        if (changed) {
            plugin.debug("Added missing default config keys.");
            plugin.saveConfig();
        }
    }

    public void toggleBuildMode(UUID uuid){
        if (buildMode.contains(uuid)){
            buildMode.remove(uuid);
        } else {
            buildMode.add(uuid);
        }
    }

    public boolean isBuildMode(UUID uuid){
        return buildMode.contains(uuid);
    }

    public String getServerPrefix(){
        return config.getString("ServerPrefix");
    }

    public double getBridgeFightVoidLimit(){
        return config.getDouble("BridgeFightVoidLimit");
    }

    public void loadWorld(String name, WorldType worldType) {
        WorldCreator creator = new WorldCreator(name);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.type(worldType);
        World world = Bukkit.createWorld(creator);

        if (world != null)
            getLogger().info("Successfully loaded world: " + world.getName());
        else
            getLogger().warning("Failed to load world: " + name);
    }


}


