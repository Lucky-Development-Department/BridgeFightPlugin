package me.molfordan.arenaAndFFAManager.manager;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.database.*;

import java.sql.*;
import java.util.*;

public class BedFightHotbarDataManager {

    private final ArenaAndFFAManager plugin;

    public BedFightHotbarDataManager(ArenaAndFFAManager plugin) {
        this.plugin = plugin;

        // Delay table generation by 1 tick
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                setupSQLTables();
            }
        });
    }

    private void setupSQLTables() {
        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();

        if (!(connector instanceof SQLDatabaseConnector)) return;

        SQLDatabaseConnector sql = (SQLDatabaseConnector) connector;

        try (Connection conn = sql.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS bedfight_hotbars (" +
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

    public Map<Integer, String> load(UUID uuid) {
        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            return loadSQL(uuid);
        }

        // Add Mongo/Redis support if needed later, mirroring HotbarDataManager pattern
        plugin.getLogger().severe("[BedFightHotbarData] No database connector available for loading!");
        return new HashMap<>();
    }

    public void save(UUID uuid, Map<Integer, String> map) {
        DatabaseConnector connector = plugin.getDatabaseManager().getConnector();

        if (connector instanceof SQLDatabaseConnector) {
            saveSQL(uuid, map);
            return;
        }

        plugin.getLogger().severe("[BedFightHotbarData] No database connector available for saving!");
    }

    private Map<Integer, String> loadSQL(UUID uuid) {
        Map<Integer, String> out = new HashMap<>();

        try {
            SQLDatabaseConnector sql = (SQLDatabaseConnector) plugin.getDatabaseManager().getConnector();

            Connection conn = sql.getConnection();
            if (conn == null || conn.isClosed()) {
                plugin.getLogger().severe("[BedFightHotbarData] SQL connection is dead!");
                return new HashMap<>();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT slot, value FROM bedfight_hotbars WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.put(rs.getInt("slot"), rs.getString("value"));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[BedFightHotbarData] SQL load error: " + e.getMessage());
        }

        return out;
    }

    private void saveSQL(UUID uuid, Map<Integer, String> map) {
        try {
            SQLDatabaseConnector sql = (SQLDatabaseConnector) plugin.getDatabaseManager().getConnector();

            Connection conn = sql.getConnection();
            if (conn == null || conn.isClosed()) {
                plugin.getLogger().severe("[BedFightHotbarData] Save aborted, SQL connection closed!");
                return;
            }

            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM bedfight_hotbars WHERE uuid=?"
            )) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bedfight_hotbars (uuid, slot, value) VALUES (?, ?, ?)"
            )) {

                for (Map.Entry<Integer, String> e : map.entrySet()) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, e.getKey());
                    ps.setString(3, e.getValue());
                    ps.addBatch();
                }

                ps.executeBatch();
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[BedFightHotbarData] SQL save failed: " + e.getMessage());
        }
    }
}
