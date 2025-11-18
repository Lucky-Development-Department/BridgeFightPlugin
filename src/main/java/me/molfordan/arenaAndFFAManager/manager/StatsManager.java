package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.object.enums.ArenaType;
import me.molfordan.arenaAndFFAManager.database.DatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.DatabaseManager;
import me.molfordan.arenaAndFFAManager.database.MongoDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;
import me.molfordan.arenaAndFFAManager.object.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.IllegalPluginAccessException;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

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
                        "build_highest_streak INT DEFAULT 0" +
                        ");";

        try (Connection conn = sql.getConnection();
             Statement st = conn.createStatement()) {

            st.executeUpdate(create);
            plugin.debug("[Stats] SQL table ready.");

        } catch (Exception e) {
            plugin.debug("[Stats] Failed to create SQL table");
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

                    return stats;
                }
            }

            // create new entry
            try (PreparedStatement insert =
                         conn.prepareStatement("INSERT INTO player_stats (uuid, username) VALUES (?, ?)")) {

                insert.setString(1, uuid.toString());
                insert.setString(2, username);
                insert.executeUpdate();
            }

            return new PlayerStats(uuid, username);

        } catch (Exception e) {
            plugin.debug("[Stats] SQL load failed → using YAML");
            return loadFromYAML(uuid, username);
        }
    }

    private PlayerStats loadFromMongo(UUID uuid, String username) {
        MongoDatabaseConnector mongo = (MongoDatabaseConnector) db.getConnector();

        try {
            org.bson.Document doc = mongo.getMongoDatabase()
                    .getCollection("player_stats")
                    .find(new org.bson.Document("uuid", uuid.toString()))
                    .first();

            if (doc != null) {
                PlayerStats stats = new PlayerStats(uuid, username);

                stats.setUsername(doc.getString("username"));

                stats.setBridgeKills(doc.getInteger("bridge_kills", 0));
                stats.setBridgeDeaths(doc.getInteger("bridge_deaths", 0));
                stats.setBridgeStreak(doc.getInteger("bridge_streak", 0));
                stats.setBridgeHighestStreak(doc.getInteger("bridge_highest_streak", 0));

                stats.setBuildKills(doc.getInteger("build_kills", 0));
                stats.setBuildDeaths(doc.getInteger("build_deaths", 0));
                stats.setBuildStreak(doc.getInteger("build_streak", 0));
                stats.setBuildHighestStreak(doc.getInteger("build_highest_streak", 0));

                return stats;
            }

            // new user
            mongo.getMongoDatabase()
                    .getCollection("player_stats")
                    .insertOne(new org.bson.Document()
                            .append("uuid", uuid.toString())
                            .append("username", username)
                    );

            return new PlayerStats(uuid, username);

        } catch (Exception e) {
            return loadFromYAML(uuid, username);
        }
    }

    private PlayerStats loadFromYAML(UUID uuid, String username) {
        File file = new File(statsFolder, uuid.toString() + ".yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        PlayerStats stats = new PlayerStats(uuid, username);

        if (!file.exists()) {
            saveToYAML(stats);
            return stats;
        }

        stats.setUsername(cfg.getString("username", username));

        stats.setBridgeKills(cfg.getInt("bridge.kills", 0));
        stats.setBridgeDeaths(cfg.getInt("bridge.deaths", 0));
        stats.setBridgeStreak(cfg.getInt("bridge.streak", 0));
        stats.setBridgeHighestStreak(cfg.getInt("bridge.highest_streak", 0));

        stats.setBuildKills(cfg.getInt("build.kills", 0));
        stats.setBuildDeaths(cfg.getInt("build.deaths", 0));
        stats.setBuildStreak(cfg.getInt("build.streak", 0));
        stats.setBuildHighestStreak(cfg.getInt("build.highest_streak", 0));

        return stats;
    }

    // --------------------------------------------------------------------------------------
    // SAVE PLAYER
    // --------------------------------------------------------------------------------------
    public void savePlayerAsync(PlayerStats stats) {
        if (stats == null) return;

        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayer(stats));
        } catch (IllegalPluginAccessException e) {
            savePlayer(stats);
        }
    }

    private void savePlayer(PlayerStats stats) {
        DatabaseConnector connector = db.getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            saveToSQL(stats);
            return;
        }

        if (connector instanceof MongoDatabaseConnector) {
            saveToMongo(stats);
            return;
        }

        saveToYAML(stats);
    }

    private void saveToSQL(PlayerStats s) {
        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();

        String update = "UPDATE player_stats SET username=?, " +
                "bridge_kills=?, bridge_deaths=?, bridge_streak=?, bridge_highest_streak=?," +
                "build_kills=?, build_deaths=?, build_streak=?, build_highest_streak=? " +
                "WHERE uuid=?";

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(update)) {

            ps.setString(1, s.getUsername());

            ps.setInt(2, s.getBridgeKills());
            ps.setInt(3, s.getBridgeDeaths());
            ps.setInt(4, s.getBridgeStreak());
            ps.setInt(5, s.getBridgeHighestStreak());

            ps.setInt(6, s.getBuildKills());
            ps.setInt(7, s.getBuildDeaths());
            ps.setInt(8, s.getBuildStreak());
            ps.setInt(9, s.getBuildHighestStreak());

            ps.setString(10, s.getUuid().toString());

            ps.executeUpdate();

        } catch (Exception e) {
            saveToYAML(s);
        }
    }

    private void saveToMongo(PlayerStats s) {
        MongoDatabaseConnector mongo = (MongoDatabaseConnector) db.getConnector();

        try {
            mongo.getMongoDatabase()
                    .getCollection("player_stats")
                    .updateOne(
                            new org.bson.Document("uuid", s.getUuid().toString()),
                            new org.bson.Document("$set", new org.bson.Document()
                                    .append("username", s.getUsername())

                                    .append("bridge_kills", s.getBridgeKills())
                                    .append("bridge_deaths", s.getBridgeDeaths())
                                    .append("bridge_streak", s.getBridgeStreak())
                                    .append("bridge_highest_streak", s.getBridgeHighestStreak())

                                    .append("build_kills", s.getBuildKills())
                                    .append("build_deaths", s.getBuildDeaths())
                                    .append("build_streak", s.getBuildStreak())
                                    .append("build_highest_streak", s.getBuildHighestStreak())
                            ),
                            new com.mongodb.client.model.UpdateOptions().upsert(true)
                    );

        } catch (Exception e) {
            saveToYAML(s);
        }
    }

    private void saveToYAML(PlayerStats s) {
        File file = new File(statsFolder, s.getUuid().toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("username", s.getUsername());

        cfg.set("bridge.kills", s.getBridgeKills());
        cfg.set("bridge.deaths", s.getBridgeDeaths());
        cfg.set("bridge.streak", s.getBridgeStreak());
        cfg.set("bridge.highest_streak", s.getBridgeHighestStreak());

        cfg.set("build.kills", s.getBuildKills());
        cfg.set("build.deaths", s.getBuildDeaths());
        cfg.set("build.streak", s.getBuildStreak());
        cfg.set("build.highest_streak", s.getBuildHighestStreak());

        try {
            cfg.save(file);
        } catch (IOException ignored) {}
    }


    // --------------------------------------------------------------------------------------
    // STREAK HELPERS
    // --------------------------------------------------------------------------------------
    public int addKillToStreak(UUID uuid, ArenaType type) {
        PlayerStats s = cache.get(uuid);
        if (s == null) return 0;

        int currentStreak = type == ArenaType.FFA ? s.getBridgeStreak() : s.getBuildStreak();
        savePlayerAsync(s); // Save the current state
        return currentStreak; // Return the current streak before increment
    }

    public void resetAllPlayerStreaks() {
        for (Map.Entry<UUID, PlayerStats> entry : cache.entrySet()) {
            PlayerStats stats = entry.getValue();
            if (stats != null) {
                stats.setBridgeStreak(0);
                stats.setBuildStreak(0);
                savePlayerAsync(stats);
            }
        }
    }

    public void resetStreak(UUID uuid, ArenaType type) {
        PlayerStats s = cache.get(uuid);
        if (s == null) return;

        if (type == ArenaType.FFA)
            s.resetBridgeStreak();
        else if (type == ArenaType.FFABUILD)
            s.resetBuildStreak();

        savePlayerAsync(s);
    }

    public void saveAllAsync() {
        if (!plugin.isEnabled()) {
            saveAllSync(); // fallback to sync if plugin is shutting down
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                saveAllSync();
            }
        });
    }

    public void saveAllSync() {
        for (PlayerStats stats : cache.values()) {
            savePlayer(stats);
        }
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.get(uuid);
    }

    /** Returns cached stats, or loads if absent */
    public PlayerStats getOrLoad(UUID uuid, String username) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) return stats;
        return loadPlayer(uuid, username);
    }

    /** Checks if player stats are loaded */
    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /** Safe remove from cache (called on quit) */
    public void unload(UUID uuid) {
        PlayerStats stats = cache.remove(uuid);
        if (stats != null) savePlayerAsync(stats);
    }

    /** Returns immutable snapshot of cached stats */
    public Collection<PlayerStats> getAllCached() {
        if (cache.isEmpty()) {
            // If cache is empty, try to load all players
            List<PlayerStats> allPlayers = getAllPlayers();
            for (PlayerStats stats : allPlayers) {
                cache.putIfAbsent(stats.getUuid(), stats);
            }
        }
        return Collections.unmodifiableCollection(cache.values());
    }

    public List<PlayerStats> getAllPlayers() {
        DatabaseConnector connector = db.getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            return loadAllPlayersFromSQL();
        }
        if (connector instanceof MongoDatabaseConnector) {
            return loadAllPlayersFromMongo();
        }
        return loadAllPlayersFromYAML();
    }

    private List<PlayerStats> loadAllPlayersFromSQL() {
        List<PlayerStats> list = new ArrayList<>();
        SQLDatabaseConnector sql = (SQLDatabaseConnector) db.getConnector();

        String query = "SELECT * FROM player_stats";

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
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

                list.add(stats);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }


    private List<PlayerStats> loadAllPlayersFromMongo() {
        List<PlayerStats> list = new ArrayList<>();
        MongoDatabaseConnector mongo = (MongoDatabaseConnector) db.getConnector();

        for (org.bson.Document doc : mongo.getMongoDatabase()
                .getCollection("player_stats")
                .find()) {

            UUID uuid = UUID.fromString(doc.getString("uuid"));
            PlayerStats stats = new PlayerStats(uuid, doc.getString("username"));

            stats.setBridgeKills(doc.getInteger("bridge_kills", 0));
            stats.setBridgeDeaths(doc.getInteger("bridge_deaths", 0));
            stats.setBridgeStreak(doc.getInteger("bridge_streak", 0));
            stats.setBridgeHighestStreak(doc.getInteger("bridge_highest_streak", 0));

            stats.setBuildKills(doc.getInteger("build_kills", 0));
            stats.setBuildDeaths(doc.getInteger("build_deaths", 0));
            stats.setBuildStreak(doc.getInteger("build_streak", 0));
            stats.setBuildHighestStreak(doc.getInteger("build_highest_streak", 0));

            list.add(stats);
        }

        return list;
    }


    private List<PlayerStats> loadAllPlayersFromYAML() {
        List<PlayerStats> list = new ArrayList<>();
        File folder = new File(plugin.getDataFolder(), "playerstats");

        if (!folder.exists()) return list;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return list;

        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                PlayerStats stats = new PlayerStats(uuid, cfg.getString("username", "Unknown"));

                stats.setBridgeKills(cfg.getInt("bridge.kills"));
                stats.setBridgeDeaths(cfg.getInt("bridge.deaths"));
                stats.setBridgeStreak(cfg.getInt("bridge.streak"));
                stats.setBridgeHighestStreak(cfg.getInt("bridge.highest_streak"));

                stats.setBuildKills(cfg.getInt("build.kills"));
                stats.setBuildDeaths(cfg.getInt("build.deaths"));
                stats.setBuildStreak(cfg.getInt("build.streak"));
                stats.setBuildHighestStreak(cfg.getInt("build.highest_streak"));

                list.add(stats);

            } catch (Exception ignored) {}
        }

        return list;
    }




}
