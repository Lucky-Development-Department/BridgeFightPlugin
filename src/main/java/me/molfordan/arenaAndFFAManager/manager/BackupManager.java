package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import me.molfordan.arenaAndFFAManager.database.DatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.MongoDatabaseConnector;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manages backup creation and restoration for database data.
 * Creates automatic backups and manual backups.
 */
public class BackupManager {
    
    private final ArenaAndFFAManager plugin;
    private final File backupFolder;
    private final int maxBackups = 10; // Keep only last 10 backups
    
    public BackupManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        initializeBackupFolder();
    }
    
    /**
     * Initialize backup directory
     */
    private void initializeBackupFolder() {
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }
    
    /**
     * Create backup of current database data
     */
    public String createBackup(String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupName = "backup_" + reason + "_" + timestamp + ".yml";
        File backupFile = new File(backupFolder, backupName);
        
        try {
            // Get current database data
            Map<UUID, PlayerStats> allStats = getAllPlayersFromDatabase();
            
            if (allStats.isEmpty()) {
                plugin.getLogger().info("No data to backup");
                return null;
            }
            
            // Create backup file
            FileConfiguration backupConfig = new YamlConfiguration();
            
            for (PlayerStats stats : allStats.values()) {
                String path = "players." + stats.getUuid().toString() + ".";
                
                backupConfig.set(path + "uuid", stats.getUuid().toString());
                backupConfig.set(path + "username", stats.getUsername());
                
                backupConfig.set(path + "bridgeKills", stats.getBridgeKills());
                backupConfig.set(path + "bridgeDeaths", stats.getBridgeDeaths());
                backupConfig.set(path + "bridgeStreak", stats.getBridgeStreak());
                backupConfig.set(path + "bridgeHighestStreak", stats.getBridgeHighestStreak());
                
                backupConfig.set(path + "buildKills", stats.getBuildKills());
                backupConfig.set(path + "buildDeaths", stats.getBuildDeaths());
                backupConfig.set(path + "buildStreak", stats.getBuildStreak());
                backupConfig.set(path + "buildHighestStreak", stats.getBuildHighestStreak());
                
                backupConfig.set(path + "lastUpdated", System.currentTimeMillis());
            }
            
            // Add metadata
            backupConfig.set("metadata.created", timestamp);
            backupConfig.set("metadata.reason", reason);
            backupConfig.set("metadata.player_count", allStats.size());
            backupConfig.set("metadata.plugin_version", plugin.getDescription().getVersion());
            
            backupConfig.save(backupFile);
            
            plugin.getLogger().info("Created backup: " + backupName + " (" + allStats.size() + " players)");
            
            // Clean up old backups
            cleanupOldBackups();
            
            return backupName;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup: " + backupName, e);
            return null;
        }
    }
    
    /**
     * Get all players from database
     */
    private Map<UUID, PlayerStats> getAllPlayersFromDatabase() {
        Map<UUID, PlayerStats> allStats = new HashMap<>();
        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();
        
        if (connector instanceof SQLDatabaseConnector) {
            return getAllPlayersFromSQL();
        } else if (connector instanceof MongoDatabaseConnector) {
            return getAllPlayersFromMongo();
        }
        
        return allStats;
    }
    
    private Map<UUID, PlayerStats> getAllPlayersFromSQL() {
        Map<UUID, PlayerStats> allStats = new HashMap<>();
        SQLDatabaseConnector sql = (SQLDatabaseConnector) plugin.getDatabaseManager().getConnector();
        
        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_stats");
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PlayerStats stats = new PlayerStats(uuid, rs.getString("username"));
                
                stats.setBridgeKills(rs.getInt("bridge_kills"));
                stats.setBridgeDeaths(rs.getInt("bridge_deaths"));
                stats.setBridgeStreak(rs.getInt("bridge_streak"));
                stats.setBridgeHighestStreak(rs.getInt("bridge_highest_streak"));
                
                stats.setBuildKills(rs.getInt("build_kills"));
                stats.setBuildDeaths(rs.getInt("build_deaths"));
                stats.setBuildStreak(rs.getInt("build_streak"));
                stats.setBuildHighestStreak(rs.getInt("build_highest_streak"));
                
                allStats.put(uuid, stats);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load all players from SQL", e);
        }
        
        return allStats;
    }
    
    private Map<UUID, PlayerStats> getAllPlayersFromMongo() {
        Map<UUID, PlayerStats> allStats = new HashMap<>();
        MongoDatabaseConnector mongo = (MongoDatabaseConnector) plugin.getDatabaseManager().getConnector();
        com.mongodb.client.MongoDatabase database = mongo.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> collection = database.getCollection("player_stats");
        
        for (org.bson.Document doc : collection.find()) {
            UUID uuid = UUID.fromString(doc.getString("_id"));
            PlayerStats stats = new PlayerStats(uuid, doc.getString("username"));
            
            stats.setBridgeKills(doc.getInteger("bridge_kills", 0));
            stats.setBridgeDeaths(doc.getInteger("bridge_deaths", 0));
            stats.setBridgeStreak(doc.getInteger("bridge_streak", 0));
            stats.setBridgeHighestStreak(doc.getInteger("bridge_highest_streak", 0));
            
            stats.setBuildKills(doc.getInteger("build_kills", 0));
            stats.setBuildDeaths(doc.getInteger("build_deaths", 0));
            stats.setBuildStreak(doc.getInteger("build_streak", 0));
            stats.setBuildHighestStreak(doc.getInteger("build_highest_streak", 0));
            
            allStats.put(uuid, stats);
        }
        
        return allStats;
    }
    
    /**
     * Restore data from backup file
     */
    public boolean restoreBackup(String backupName) {
        File backupFile = new File(backupFolder, backupName);
        if (!backupFile.exists()) {
            plugin.getLogger().warning("Backup file not found: " + backupName);
            return false;
        }
        
        try {
            FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
            
            if (!backupConfig.contains("players")) {
                plugin.getLogger().warning("Invalid backup file: " + backupName);
                return false;
            }
            
            int restoredCount = 0;
            StatsManager statsManager = plugin.getStatsManager();
            
            for (String uuidString : backupConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    PlayerStats stats = loadPlayerStatsFromBackup(backupConfig, uuid);
                    
                    if (stats != null) {
                        statsManager.savePlayer(stats);
                        restoredCount++;
                    }
                    
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in backup: " + uuidString);
                }
            }
            
            plugin.getLogger().info("Restored " + restoredCount + " players from backup: " + backupName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore backup: " + backupName, e);
            return false;
        }
    }
    
    /**
     * Load player stats from backup configuration
     */
    private PlayerStats loadPlayerStatsFromBackup(FileConfiguration backupConfig, UUID uuid) {
        String path = "players." + uuid.toString() + ".";
        
        try {
            PlayerStats stats = new PlayerStats(uuid, backupConfig.getString(path + "username", "Unknown"));
            
            stats.setBridgeKills(backupConfig.getInt(path + "bridgeKills", 0));
            stats.setBridgeDeaths(backupConfig.getInt(path + "bridgeDeaths", 0));
            stats.setBridgeStreak(backupConfig.getInt(path + "bridgeStreak", 0));
            stats.setBridgeHighestStreak(backupConfig.getInt(path + "bridgeHighestStreak", 0));
            
            stats.setBuildKills(backupConfig.getInt(path + "buildKills", 0));
            stats.setBuildDeaths(backupConfig.getInt(path + "buildDeaths", 0));
            stats.setBuildStreak(backupConfig.getInt(path + "buildStreak", 0));
            stats.setBuildHighestStreak(backupConfig.getInt(path + "buildHighestStreak", 0));
            
            return stats;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player stats from backup: " + uuid, e);
            return null;
        }
    }
    
    /**
     * List all available backups
     */
    public List<String> listBackups() {
        List<String> backups = new ArrayList<>();
        
        if (backupFolder.exists()) {
            File[] files = backupFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    backups.add(file.getName());
                }
            }
        }
        
        backups.sort(Collections.reverseOrder());
        return backups;
    }
    
    /**
     * Delete a backup file
     */
    public boolean deleteBackup(String backupName) {
        File backupFile = new File(backupFolder, backupName);
        if (backupFile.exists() && backupFile.delete()) {
            plugin.getLogger().info("Deleted backup: " + backupName);
            return true;
        }
        return false;
    }
    
    /**
     * Clean up old backups, keeping only the most recent ones
     */
    private void cleanupOldBackups() {
        List<String> backups = listBackups();
        
        while (backups.size() > maxBackups) {
            String oldestBackup = backups.remove(backups.size() - 1);
            deleteBackup(oldestBackup);
        }
    }
    
    /**
     * Create compressed backup archive
     */
    public String createCompressedBackup(String reason) {
        String backupName = createBackup(reason);
        if (backupName == null) return null;
        
        String zipName = backupName.replace(".yml", ".zip");
        File backupFile = new File(backupFolder, backupName);
        File zipFile = new File(backupFolder, zipName);
        
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            ZipEntry entry = new ZipEntry(backupName);
            zos.putNextEntry(entry);
            Files.copy(backupFile.toPath(), zos);
            zos.closeEntry();
            
            // Delete the uncompressed backup
            backupFile.delete();
            
            plugin.getLogger().info("Created compressed backup: " + zipName);
            return zipName;
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create compressed backup", e);
            return null;
        }
    }
    
    /**
     * Get backup folder path
     */
    public File getBackupFolder() {
        return backupFolder;
    }
    
    /**
     * Get backup information
     */
    public Map<String, Object> getBackupInfo(String backupName) {
        File backupFile = new File(backupFolder, backupName);
        if (!backupFile.exists()) {
            return null;
        }
        
        Map<String, Object> info = new HashMap<>();
        FileConfiguration config = YamlConfiguration.loadConfiguration(backupFile);
        
        info.put("name", backupName);
        info.put("size", backupFile.length());
        info.put("created", config.getString("metadata.created", "Unknown"));
        info.put("reason", config.getString("metadata.reason", "Unknown"));
        info.put("player_count", config.getInt("metadata.player_count", 0));
        info.put("plugin_version", config.getString("metadata.plugin_version", "Unknown"));
        
        return info;
    }
}
