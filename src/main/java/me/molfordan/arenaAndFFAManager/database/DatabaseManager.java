package me.molfordan.arenaAndFFAManager.database;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.database.connectors.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;

public class DatabaseManager {

    private final ArenaAndFFAManager plugin;
    private DatabaseConnector selectedConnector;
    private DataSource dataSource;

    public DatabaseManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;
        createDefaultFile();
        loadDatabase();
    }

    /** Creates database.yml if missing and fills missing keys with defaults */
    private void createDefaultFile() {
        File file = new File(plugin.getDataFolder(), "database.yml");

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
                System.out.println("[Database] Created new database.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;

        // Main type
        changed |= setDefault(cfg, "type", "SQLITE");

        // MySQL
        changed |= setDefault(cfg, "mysql.enabled", false);
        changed |= setDefault(cfg, "mysql.host", "localhost");
        changed |= setDefault(cfg, "mysql.port", 3306);
        changed |= setDefault(cfg, "mysql.database", "mydatabase");
        changed |= setDefault(cfg, "mysql.user", "root");
        changed |= setDefault(cfg, "mysql.password", "password");

        // Redis
        changed |= setDefault(cfg, "redis.enabled", false);
        changed |= setDefault(cfg, "redis.host", "localhost");
        changed |= setDefault(cfg, "redis.port", 6379);

        // MongoDB
        changed |= setDefault(cfg, "mongodb.enabled", false);
        changed |= setDefault(cfg, "mongodb.host", "localhost");
        changed |= setDefault(cfg, "mongodb.port", 27017);
        changed |= setDefault(cfg, "mongodb.database", "mydb");
        changed |= setDefault(cfg, "mongodb.user", "");
        changed |= setDefault(cfg, "mongodb.password", "");

        if (changed) {
            try {
                cfg.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Utility: set default if missing */
    private boolean setDefault(FileConfiguration cfg, String path, Object value) {
        if (!cfg.contains(path)) {
            cfg.set(path, value);
            return true;
        }
        return false;
    }

    /** Loads and selects database connector */
    private void loadDatabase() {

        File file = new File(plugin.getDataFolder(), "database.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // determine type using enum
        DatabaseType type;
        try {
            type = DatabaseType.valueOf(cfg.getString("type", "SQLITE").toUpperCase());
        } catch (IllegalArgumentException e) {
            type = DatabaseType.SQLITE;
        }

        boolean mysql = cfg.getBoolean("mysql.enabled");
        boolean redis = cfg.getBoolean("redis.enabled");
        boolean mongo = cfg.getBoolean("mongodb.enabled");

        // NOTHING enabled → auto fallback to SQLite
        if (!mysql && !redis && !mongo) {
            System.out.println("[Database] No database enabled → Using SQLite.");
            selectedConnector = new SQLiteConnector(new File(plugin.getDataFolder(), "data.db"));
            selectedConnector.connect();
            return;
        }

        switch (type) {

            case MYSQL:
                if (mysql) {
                    selectedConnector = new MySQLConnector(
                            cfg.getString("mysql.host"),
                            cfg.getInt("mysql.port"),
                            cfg.getString("mysql.database"),
                            cfg.getString("mysql.user"),
                            cfg.getString("mysql.password")
                    );
                }
                break;

            case REDIS:
                if (redis) {
                    selectedConnector = new RedisConnector(
                            cfg.getString("redis.host"),
                            cfg.getInt("redis.port")
                    );
                }
                break;

            case MONGODB:
                if (mongo) {
                    selectedConnector = new MongoDBConnector(
                            cfg.getString("mongodb.host"),
                            cfg.getInt("mongodb.port"),
                            cfg.getString("mongodb.database"),
                            cfg.getString("mongodb.user"),
                            cfg.getString("mongodb.password")
                    );
                }
                break;

            case SQLITE:
            case NONE:
            default:
                selectedConnector = new SQLiteConnector(new File(plugin.getDataFolder(), "data.db"));
                break;
        }

        // FINAL protection
        if (selectedConnector == null) {
            System.out.println("[Database] No valid connector found → Using SQLite.");
            selectedConnector = new SQLiteConnector(new File(plugin.getDataFolder(), "data.db"));
        }

        selectedConnector.connect();
    }

    public DatabaseConnector getConnector() {
        return selectedConnector;
    }

    public boolean isConnected() {
        return selectedConnector != null && selectedConnector.isConnected();
    }

    public void shutdown() {
        if (selectedConnector != null) {
            selectedConnector.disconnect();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
