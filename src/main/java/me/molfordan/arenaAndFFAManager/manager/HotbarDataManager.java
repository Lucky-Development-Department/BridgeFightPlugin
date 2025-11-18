package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.database.DatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.MongoDatabaseConnector;
import me.molfordan.arenaAndFFAManager.database.RedisDatabaseConnector;

import org.bukkit.configuration.file.YamlConfiguration;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class HotbarDataManager {

    private final ArenaAndFFAManager plugin;
    private final File dir;

    public HotbarDataManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "hotbars");
        if (!dir.exists()) dir.mkdirs();

        // Delay table generation by 1 tick
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                setupSQLTables();
            }
        });
    }

    // --------------------------------------------------------------------
    //  DATABASE TABLE (SQL ONLY)
    // --------------------------------------------------------------------
    private void setupSQLTables() {
        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();

        if (!(connector instanceof SQLDatabaseConnector)) return;

        SQLDatabaseConnector sql = (SQLDatabaseConnector) connector;

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS hotbars (" +
                             "uuid VARCHAR(36)," +
                             "slot INT," +
                             "value TEXT," +
                             "PRIMARY KEY(uuid, slot))"
             )) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------
    //  LOAD
    // --------------------------------------------------------------------
    public Map<Integer, String> load(UUID uuid) {

        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            return loadSQL(uuid);
        }

        if (connector instanceof MongoDatabaseConnector) {
            return loadMongo(uuid);
        }

        if (connector instanceof RedisDatabaseConnector) {
            return loadRedis(uuid);
        }

        return loadYAML(uuid);
    }

    // --------------------------------------------------------------------
    //  SAVE
    // --------------------------------------------------------------------
    public void save(UUID uuid, Map<Integer, String> map) {

        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            saveSQL(uuid, map);
            return;
        }

        if (connector instanceof MongoDatabaseConnector) {
            saveMongo(uuid, map);
            return;
        }

        if (connector instanceof RedisDatabaseConnector) {
            saveRedis(uuid, map);
            return;
        }

        saveYAML(uuid, map);
    }

    // --------------------------------------------------------------------
    //  SQL IMPLEMENTATION
    // --------------------------------------------------------------------
    private Map<Integer, String> loadSQL(UUID uuid) {
        Map<Integer, String> out = new HashMap<>();

        try {
            SQLDatabaseConnector sql = (SQLDatabaseConnector) plugin.getDatabaseManager().getConnector();
            Connection conn = sql.getConnection();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT slot, value FROM hotbars WHERE uuid = ?"
            );
            ps.setString(1, uuid.toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.put(rs.getInt("slot"), rs.getString("value"));
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }

    private void saveSQL(UUID uuid, Map<Integer, String> map) {
        try {
            SQLDatabaseConnector sql = (SQLDatabaseConnector) plugin.getDatabaseManager().getConnector();
            Connection conn = sql.getConnection();

            PreparedStatement wipe = conn.prepareStatement("DELETE FROM hotbars WHERE uuid = ?");
            wipe.setString(1, uuid.toString());
            wipe.executeUpdate();
            wipe.close();

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO hotbars (uuid, slot, value) VALUES (?,?,?)"
            );

            for (Map.Entry<Integer, String> e : map.entrySet()) {
                insert.setString(1, uuid.toString());
                insert.setInt(2, e.getKey());
                insert.setString(3, e.getValue());
                insert.addBatch();
            }

            insert.executeBatch();
            insert.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------
    //  YAML FALLBACK
    // --------------------------------------------------------------------
    private Map<Integer, String> loadYAML(UUID uuid) {
        File f = new File(dir, uuid.toString() + ".yml");
        if (!f.exists()) return new HashMap<>();

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        Map<Integer, String> out = new HashMap<>();

        for (String key : cfg.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                String val = cfg.getString(key);
                if (val != null) out.put(slot, val);
            } catch (Exception ignored) {}
        }
        return out;
    }

    private void saveYAML(UUID uuid, Map<Integer, String> map) {
        File f = new File(dir, uuid.toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<Integer, String> e : map.entrySet()) {
            cfg.set(String.valueOf(e.getKey()), e.getValue());
        }

        try {
            cfg.save(f);
        } catch (IOException ex) {
            plugin.debug("Could not save hotbar for " + uuid + ": " + ex.getMessage());
        }
    }

    // --------------------------------------------------------------------
    //  REDIS
    // --------------------------------------------------------------------
    private Map<Integer, String> loadRedis(UUID uuid) {
        Map<Integer, String> out = new HashMap<>();

        try {
            RedisDatabaseConnector redisConnector =
                    (RedisDatabaseConnector) plugin.getDatabaseManager().getConnector();

            Jedis jedis = redisConnector.getRedisClient();

            Map<String, String> redisMap = jedis.hgetAll("hotbar:" + uuid);

            for (Map.Entry<String, String> e : redisMap.entrySet()) {
                out.put(Integer.parseInt(e.getKey()), e.getValue());
            }

        } catch (Exception ignored) {}

        return out;
    }

    private void saveRedis(UUID uuid, Map<Integer, String> map) {
        try {
            RedisDatabaseConnector redisConnector =
                    (RedisDatabaseConnector) plugin.getDatabaseManager().getConnector();

            Jedis jedis = redisConnector.getRedisClient();

            String key = "hotbar:" + uuid;

            jedis.del(key);

            for (Map.Entry<Integer, String> e : map.entrySet()) {
                jedis.hset(key, String.valueOf(e.getKey()), e.getValue());
            }

        } catch (Exception ignored) {}
    }

    // --------------------------------------------------------------------
    //  MONGODB
    // --------------------------------------------------------------------
    private Map<Integer, String> loadMongo(UUID uuid) {
        MongoDatabaseConnector mongoConnector =
                (MongoDatabaseConnector) plugin.getDatabaseManager().getConnector();

        com.mongodb.client.MongoDatabase mongo = mongoConnector.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> coll =
                mongo.getCollection("hotbars", org.bson.Document.class);

        org.bson.Document doc = coll.find(new org.bson.Document("_id", uuid.toString())).first();
        if (doc == null) return new HashMap<>();

        Map<Integer, String> out = new HashMap<>();
        org.bson.Document slots = doc.get("slots", org.bson.Document.class);

        for (String key : slots.keySet()) {
            out.put(Integer.parseInt(key), slots.getString(key));
        }

        return out;
    }

    private void saveMongo(UUID uuid, Map<Integer, String> map) {
        MongoDatabaseConnector mongoConnector =
                (MongoDatabaseConnector) plugin.getDatabaseManager().getConnector();

        com.mongodb.client.MongoDatabase mongo = mongoConnector.getMongoDatabase();
        com.mongodb.client.MongoCollection<org.bson.Document> coll =
                mongo.getCollection("hotbars", org.bson.Document.class);

        org.bson.Document slots = new org.bson.Document();
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            slots.append(String.valueOf(e.getKey()), e.getValue());
        }

        org.bson.Document doc = new org.bson.Document("_id", uuid.toString())
                .append("slots", slots);

        coll.replaceOne(
                new org.bson.Document("_id", uuid.toString()),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }
}
