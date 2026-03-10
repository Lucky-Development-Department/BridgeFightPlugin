package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.database.DatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.DatabaseManager;
import me.molfordan.arenaAndFFAManager.database.MongoDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.connectors.MongoDBConnector;
import me.molfordan.arenaAndFFAManager.database.connectors.MySQLConnector;
import me.molfordan.arenaAndFFAManager.database.connectors.SQLiteConnector;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.IllegalPluginAccessException;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class StatsManager {

    private final ArenaAndFFAManager plugin;
    private final DatabaseManager db;
    private final File statsFolder;

    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    
    public StatsManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();

        this.statsFolder = new File(plugin.getDataFolder(), "playerstats");
        if (!statsFolder.exists()) statsFolder.mkdirs();

        setupSQLTables();
    }

    // --------------------------------------------------------------------------------------
    // SQL TABLE SETUP
    // --------------------------------------------------------------------------------------
    private void setupSQLTables() {
        if (!(db.getConnector() instanceof SQLDatabaseConnector)) return;

        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();

        String create =
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "username VARCHAR(32)," +

                        "bridge_kills INT DEFAULT 0," +
                        "bridge_deaths INT DEFAULT 0," +
                        "bridge_streak INT DEFAULT 0," +
                        "bridge_highest_streak INT DEFAULT 0," +

                        "build_kills INT DEFAULT 0," +
                        "build_deaths INT DEFAULT 0," +
                        "build_streak INT DEFAULT 0," +
                        "build_highest_streak INT DEFAULT 0," +
                        "last_selected_bridge_kit VARCHAR(64) DEFAULT 'Default'" +
                        ");";

        try (Connection conn = sql.getConnection();
             Statement st = conn.createStatement()) {

            st.executeUpdate(create);
            //plugin.debug("[Stats] SQL table ready.");
            
            // Add missing columns for existing tables
            addMissingColumns(conn);

        } catch (Exception e) {
            //plugin.debug("[Stats] Failed to create SQL table");
        }
    }
    
    private void addMissingColumns(Connection conn) {
        try (Statement st = conn.createStatement()) {
            // Check and add last_selected_bridge_kit if missing
            try {
                st.executeQuery("SELECT last_selected_bridge_kit FROM player_stats LIMIT 1");
            } catch (SQLException e) {
                //plugin.debug("[Stats] Adding missing last_selected_bridge_kit column");
                st.executeUpdate("ALTER TABLE player_stats ADD COLUMN last_selected_bridge_kit VARCHAR(64) DEFAULT 'Default'");
            }
            
            //plugin.debug("[Stats] Column migration completed");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to add missing columns to SQL table", e);
        }
    }

    // --------------------------------------------------------------------------------------
    // LOAD PLAYER
    // --------------------------------------------------------------------------------------
    public PlayerStats loadPlayer(UUID uuid, String username) {
        if (cache.containsKey(uuid)) return cache.get(uuid);

        DatabaseConnector connector = db.getConnector();
        PlayerStats stats;

        if (connector instanceof SQLDatabaseConnector) {
            stats = loadFromSQL(uuid, username);
        } else if (connector instanceof MongoDatabaseConnector) {
            stats = loadFromMongo(uuid, username);
        } else {
            stats = loadFromYAML(uuid, username);
        }

        cache.put(uuid, stats);
        return stats;
    }

    private PlayerStats loadFromSQL(UUID uuid, String username) {
        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_stats WHERE uuid=?")) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(uuid, username);

                    stats.setUsername(rs.getString("username"));

                    stats.setBridgeKills(rs.getInt("bridge_kills"));
                    stats.setBridgeDeaths(rs.getInt("bridge_deaths"));
                    stats.setBridgeStreak(rs.getInt("bridge_streak"));
                    stats.setBridgeHighestStreak(rs.getInt("bridge_highest_streak"));

                    stats.setBuildKills(rs.getInt("build_kills"));
                    stats.setBuildDeaths(rs.getInt("build_deaths"));
                    stats.setBuildStreak(rs.getInt("build_streak"));
                    stats.setBuildHighestStreak(rs.getInt("build_highest_streak"));
                    stats.setLastSelectedBridgeKit(rs.getString("last_selected_bridge_kit"));

                    return stats;
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player stats from SQL", e);
        }

        return new PlayerStats(uuid, username);
    }

    private PlayerStats loadFromMongo(UUID uuid, String username) {
        MongoDBConnector mongo = (MongoDBConnector) db.getConnector();
        com.mongodb.client.MongoDatabase database = mongo.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> collection = database.getCollection("player_stats");

        org.bson.Document doc = collection.find(new org.bson.Document("_id", uuid.toString())).first();
        if (doc != null) {
            PlayerStats stats = new PlayerStats(uuid, username);
            stats.setUsername(doc.getString("username"));

            stats.setBridgeKills(doc.getInteger("bridge_kills", 0));
            stats.setBridgeDeaths(doc.getInteger("bridge_deaths", 0));
            stats.setBridgeStreak(doc.getInteger("bridge_streak", 0));
            stats.setBridgeHighestStreak(doc.getInteger("bridge_highest_streak", 0));
            stats.setBridgeDailyStreak(doc.getInteger("bridge_daily_streak", 0));
            stats.setBridgeDailyHighestStreak(doc.getInteger("bridge_daily_highest_streak", 0));

            stats.setBuildKills(doc.getInteger("build_kills", 0));
            stats.setBuildDeaths(doc.getInteger("build_deaths", 0));
            stats.setBuildStreak(doc.getInteger("build_streak", 0));
            stats.setBuildHighestStreak(doc.getInteger("build_highest_streak", 0));
            stats.setBuildDailyStreak(doc.getInteger("build_daily_streak", 0));
            stats.setBuildDailyHighestStreak(doc.getInteger("build_daily_highest_streak", 0));
            String kitName = doc.getString("last_selected_bridge_kit");
            stats.setLastSelectedBridgeKit(kitName != null ? kitName : "Default");

            return stats;
        }

        return new PlayerStats(uuid, username);
    }

    private PlayerStats loadFromYAML(UUID uuid, String username) {
        File file = new File(statsFolder, uuid.toString() + ".yml");
        if (!file.exists()) return new PlayerStats(uuid, username);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerStats stats = new PlayerStats(uuid, username);

        stats.setUsername(config.getString("username", username));

        stats.setBridgeKills(config.getInt("bridge_kills", 0));
        stats.setBridgeDeaths(config.getInt("bridge_deaths", 0));
        stats.setBridgeStreak(config.getInt("bridge_streak", 0));
        stats.setBridgeHighestStreak(config.getInt("bridge_highest_streak", 0));
        stats.setBridgeDailyStreak(config.getInt("bridge_daily_streak", 0));
        stats.setBridgeDailyHighestStreak(config.getInt("bridge_daily_highest_streak", 0));

        stats.setBuildKills(config.getInt("build_kills", 0));
        stats.setBuildDeaths(config.getInt("build_deaths", 0));
        stats.setBuildStreak(config.getInt("build_streak", 0));
        stats.setBuildHighestStreak(config.getInt("build_highest_streak", 0));
        stats.setBuildDailyStreak(config.getInt("build_daily_streak", 0));
        stats.setBuildDailyHighestStreak(config.getInt("build_daily_highest_streak", 0));
        stats.setLastSelectedBridgeKit(config.getString("last_selected_bridge_kit", "Default"));

        return stats;
    }

    // --------------------------------------------------------------------------------------
    // SAVE PLAYER
    // --------------------------------------------------------------------------------------
    public void savePlayer(PlayerStats stats) {
        DatabaseConnector connector = db.getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            saveToSQL(stats);
        } else if (connector instanceof MongoDatabaseConnector) {
            saveToMongo(stats);
        } else {
            saveToYAML(stats);
        }

        cache.put(stats.getUuid(), stats);
    }

    private void saveToSQL(PlayerStats stats) {
        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_stats (uuid, username, bridge_kills, bridge_deaths, bridge_streak, bridge_highest_streak, " +
                             "build_kills, build_deaths, build_streak, build_highest_streak, last_selected_bridge_kit) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE username=?, bridge_kills=?, bridge_deaths=?, bridge_streak=?, bridge_highest_streak=?, " +
                             "build_kills=?, build_deaths=?, build_streak=?, build_highest_streak=?, last_selected_bridge_kit=?"
             )) {

            String uuid = stats.getUuid().toString();
            ps.setString(1, uuid);
            ps.setString(2, stats.getUsername());
            ps.setInt(3, stats.getBridgeKills());
            ps.setInt(4, stats.getBridgeDeaths());
            ps.setInt(5, stats.getBridgeStreak());
            ps.setInt(6, stats.getBridgeHighestStreak());
            ps.setInt(7, stats.getBuildKills());
            ps.setInt(8, stats.getBuildDeaths());
            ps.setInt(9, stats.getBuildStreak());
            ps.setInt(10, stats.getBuildHighestStreak());
            ps.setString(11, stats.getLastSelectedBridgeKit() != null ? stats.getLastSelectedBridgeKit() : "Default");

            ps.setString(12, stats.getUsername());
            ps.setInt(13, stats.getBridgeKills());
            ps.setInt(14, stats.getBridgeDeaths());
            ps.setInt(15, stats.getBridgeStreak());
            ps.setInt(16, stats.getBridgeHighestStreak());
            ps.setInt(17, stats.getBuildKills());
            ps.setInt(18, stats.getBuildDeaths());
            ps.setInt(19, stats.getBuildStreak());
            ps.setInt(20, stats.getBuildHighestStreak());
            ps.setString(21, stats.getLastSelectedBridgeKit() != null ? stats.getLastSelectedBridgeKit() : "Default");

            ps.executeUpdate();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player stats to SQL", e);
        }
    }

    private void saveToMongo(PlayerStats stats) {
        MongoDBConnector mongo = (MongoDBConnector) db.getConnector();
        com.mongodb.client.MongoDatabase database = mongo.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> collection = database.getCollection("player_stats");

        org.bson.Document doc = new org.bson.Document("_id", stats.getUuid().toString())
                .append("username", stats.getUsername())
                .append("bridge_kills", stats.getBridgeKills())
                .append("bridge_deaths", stats.getBridgeDeaths())
                .append("bridge_streak", stats.getBridgeStreak())
                .append("bridge_highest_streak", stats.getBridgeHighestStreak())
                .append("bridge_daily_streak", stats.getBridgeDailyStreak())
                .append("bridge_daily_highest_streak", stats.getBridgeDailyHighestStreak())
                .append("build_kills", stats.getBuildKills())
                .append("build_deaths", stats.getBuildDeaths())
                .append("build_streak", stats.getBuildStreak())
                .append("build_highest_streak", stats.getBuildHighestStreak())
                .append("build_daily_streak", stats.getBuildDailyStreak())
                .append("build_daily_highest_streak", stats.getBuildDailyHighestStreak())
                .append("last_selected_bridge_kit", stats.getLastSelectedBridgeKit() != null ? stats.getLastSelectedBridgeKit() : "Default");

        collection.replaceOne(
                new org.bson.Document("_id", stats.getUuid().toString()),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }

    private void saveToYAML(PlayerStats stats) {
        File file = new File(statsFolder, stats.getUuid().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("username", stats.getUsername());
        config.set("bridge_kills", stats.getBridgeKills());
        config.set("bridge_deaths", stats.getBridgeDeaths());
        config.set("bridge_streak", stats.getBridgeStreak());
        config.set("bridge_highest_streak", stats.getBridgeHighestStreak());
        config.set("bridge_daily_streak", stats.getBridgeDailyStreak());
        config.set("bridge_daily_highest_streak", stats.getBridgeDailyHighestStreak());
        config.set("build_kills", stats.getBuildKills());
        config.set("build_deaths", stats.getBuildDeaths());
        config.set("build_streak", stats.getBuildStreak());
        config.set("build_highest_streak", stats.getBuildHighestStreak());
        config.set("build_daily_streak", stats.getBuildDailyStreak());
        config.set("build_daily_highest_streak", stats.getBuildDailyHighestStreak());
        config.set("last_selected_bridge_kit", stats.getLastSelectedBridgeKit() != null ? stats.getLastSelectedBridgeKit() : "Default");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player stats to YAML", e);
        }
    }

    // --------------------------------------------------------------------------------------
    // STATS MANAGEMENT
    // --------------------------------------------------------------------------------------
    public void addKill(UUID uuid, ArenaType type) {
        PlayerStats stats = loadPlayer(uuid, null);
        if (type == ArenaType.BRIDGE) {
            stats.setBridgeKills(stats.getBridgeKills() + 1);
            stats.setBridgeStreak(stats.getBridgeStreak() + 1);
            if (stats.getBridgeStreak() > stats.getBridgeHighestStreak()) {
                stats.setBridgeHighestStreak(stats.getBridgeStreak());
            }
        } else if (type == ArenaType.BUILD) {
            stats.setBuildKills(stats.getBuildKills() + 1);
            stats.setBuildStreak(stats.getBuildStreak() + 1);
            if (stats.getBuildStreak() > stats.getBuildHighestStreak()) {
                stats.setBuildHighestStreak(stats.getBuildStreak());
            }
        }
        savePlayer(stats);
    }

    public void addDeath(UUID uuid, ArenaType type) {
        PlayerStats stats = loadPlayer(uuid, null);
        if (type == ArenaType.BRIDGE) {
            stats.setBridgeDeaths(stats.getBridgeDeaths() + 1);
            stats.setBridgeStreak(0);
        } else if (type == ArenaType.BUILD) {
            stats.setBuildDeaths(stats.getBuildDeaths() + 1);
            stats.setBuildStreak(0);
        }
        savePlayer(stats);
    }

    public PlayerStats getStats(UUID uuid) {
        return loadPlayer(uuid, null);
    }

    public void resetStats(UUID uuid) {
        PlayerStats stats = loadPlayer(uuid, null);
        stats.setBridgeKills(0);
        stats.setBridgeDeaths(0);
        stats.setBridgeStreak(0);
        stats.setBridgeHighestStreak(0);
        stats.setBuildKills(0);
        stats.setBuildDeaths(0);
        stats.setBuildStreak(0);
        stats.setBuildHighestStreak(0);
        savePlayer(stats);
    }

    // --------------------------------------------------------------------------------------
    // LEADERBOARD
    // --------------------------------------------------------------------------------------
    public List<PlayerStats> getTopPlayers(ArenaType type, String stat, int limit) {
        List<PlayerStats> topPlayers = new ArrayList<>();
        DatabaseConnector connector = db.getConnector();
        
        //plugin.debug("[Stats] Getting top players for " + type + " " + stat + " (limit: " + limit + ")");
        //plugin.debug("[Stats] Using connector: " + connector.getClass().getSimpleName());
        //plugin.debug("[Stats] Connector connected: " + connector.isConnected());

        if (connector instanceof SQLDatabaseConnector) {
            topPlayers = getTopPlayersSQL(type, stat, limit);
        } else if (connector instanceof MongoDatabaseConnector) {
            topPlayers = getTopPlayersMongo(type, stat, limit);
        } else {
            //plugin.debug("[Stats] No specific connector found, using YAML fallback");
        }

        //plugin.debug("[Stats] Retrieved " + topPlayers.size() + " players");
        return topPlayers;
    }

    private List<PlayerStats> getTopPlayersSQL(ArenaType type, String stat, int limit) {
        List<PlayerStats> topPlayers = new ArrayList<>();
        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();

        String column;
        if (type == ArenaType.BRIDGE) {
            column = "bridge_" + stat;
        } else {
            column = "build_" + stat;
        }
        
        String query = "SELECT uuid, username, " + column + " FROM player_stats ORDER BY " + column + " DESC LIMIT ?";
        //plugin.debug("[Stats] SQL Query: " + query);

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, limit);
            //plugin.debug("[Stats] Executing query with limit: " + limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String username = rs.getString("username");
                    PlayerStats stats = loadPlayer(uuid, username);
                    topPlayers.add(stats);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get top players from SQL", e);
            //plugin.debug("[Stats] SQL Error: " + e.getMessage());
        }

        //plugin.debug("[Stats] SQL returned " + topPlayers.size() + " results");
        return topPlayers;
    }

    private List<PlayerStats> getTopPlayersMongo(ArenaType type, String stat, int limit) {
        List<PlayerStats> topPlayers = new ArrayList<>();
        MongoDBConnector mongo = (MongoDBConnector) db.getConnector();
        com.mongodb.client.MongoDatabase database = mongo.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> collection = database.getCollection("player_stats");

        String field;
        if (type == ArenaType.BRIDGE) {
            field = "bridge_" + stat;
        } else {
            field = "build_" + stat;
        }

        org.bson.Document sort = new org.bson.Document(field, -1);
        for (org.bson.Document doc : collection.find().sort(sort).limit(limit)) {
            UUID uuid = UUID.fromString(doc.getString("_id"));
            String username = doc.getString("username");
            PlayerStats stats = loadPlayer(uuid, username);
            topPlayers.add(stats);
        }

        return topPlayers;
    }

    // --------------------------------------------------------------------------------------
    // CACHE MANAGEMENT
    // --------------------------------------------------------------------------------------
    public void clearCache() {
        cache.clear();
    }

    public void removeFromCache(UUID uuid) {
        cache.remove(uuid);
    }

    public Map<UUID, PlayerStats> getCache() {
        return new HashMap<>(cache);
    }

    // --------------------------------------------------------------------------------------
    // ADDITIONAL METHODS FOR COMPATIBILITY
    // --------------------------------------------------------------------------------------
    public PlayerStats getOrLoad(UUID uuid, String username) {
        return loadPlayer(uuid, username);
    }

    public void savePlayerAsync(PlayerStats stats) {
        // For now, save synchronously. Could be made truly async later.
        savePlayer(stats);
    }

    public void resetStreak(UUID uuid, ArenaType type) {
        PlayerStats stats = loadPlayer(uuid, null);
        if (type == ArenaType.BRIDGE) {
            stats.setBridgeStreak(0);
        } else if (type == ArenaType.FFABUILD || type == ArenaType.BUILD) {
            stats.setBuildStreak(0);
        }
        savePlayer(stats);
    }

    public void addKillToStreak(UUID uuid, ArenaType type) {
        PlayerStats stats = loadPlayer(uuid, null);
        if (type == ArenaType.BRIDGE) {
            stats.setBridgeStreak(stats.getBridgeStreak() + 1);
            if (stats.getBridgeStreak() > stats.getBridgeHighestStreak()) {
                stats.setBridgeHighestStreak(stats.getBridgeStreak());
            }
        } else if (type == ArenaType.FFABUILD || type == ArenaType.BUILD) {
            stats.setBuildStreak(stats.getBuildStreak() + 1);
            if (stats.getBuildStreak() > stats.getBuildHighestStreak()) {
                stats.setBuildHighestStreak(stats.getBuildStreak());
            }
        }
        savePlayer(stats);
    }

    public void shutdown() {
        clearCache();
    }

    // --------------------------------------------------------------------------------------
    // DAILY STREAK RESET
    // --------------------------------------------------------------------------------------
    public void resetAllDailyStreaks() {
        DatabaseConnector connector = db.getConnector();
        
        plugin.getLogger().info("[Stats] Resetting all daily streaks...");
        
        if (connector instanceof SQLDatabaseConnector) {
            resetAllDailyStreaksSQL();
        } else if (connector instanceof MongoDatabaseConnector) {
            resetAllDailyStreaksMongo();
        } else {
            resetAllDailyStreaksYAML();
        }
        
        // Clear cache to force reload with reset values
        clearCache();
        
        plugin.getLogger().info("[Stats] All daily streaks have been reset.");
    }
    
    private void resetAllDailyStreaksSQL() {
        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();
        
        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE player_stats SET bridge_streak = 0, build_streak = 0"
             )) {
            
            int rowsUpdated = ps.executeUpdate();
            plugin.getLogger().info("[Stats] Reset daily streaks for " + rowsUpdated + " players in SQL database.");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset daily streaks in SQL database", e);
        }
    }
    
    private void resetAllDailyStreaksMongo() {
        MongoDBConnector mongo = (MongoDBConnector) db.getConnector();
        com.mongodb.client.MongoDatabase database = mongo.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> collection = database.getCollection("player_stats");
        
        org.bson.Document update = new org.bson.Document("$set", 
            new org.bson.Document("bridge_streak", 0)
                .append("build_streak", 0));
        
        com.mongodb.client.result.UpdateResult result = collection.updateMany(new org.bson.Document(), update);
        plugin.getLogger().info("[Stats] Reset daily streaks for " + result.getModifiedCount() + " players in MongoDB.");
    }
    
    private void resetAllDailyStreaksYAML() {
        if (!statsFolder.exists()) return;
        
        File[] files = statsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        int resetCount = 0;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("bridge_streak", 0);
                config.set("build_streak", 0);
                config.save(file);
                resetCount++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to reset daily streaks for file: " + file.getName(), e);
            }
        }
        
        plugin.getLogger().info("[Stats] Reset daily streaks for " + resetCount + " players in YAML files.");
    }
}
