package me.molfordan.arenaAndFFAManager.database.connectors;

import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnector implements SQLDatabaseConnector {

    private final File databaseFile;
    private boolean driverLoaded = false;

    public SQLiteConnector(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public void connect() {
        // 1. Load SQLite driver
        try {
            Class.forName("org.sqlite.JDBC");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.out.println("[SQLite] Failed to load SQLite driver (org.sqlite.JDBC)!");
            e.printStackTrace();
            return;
        }

        // 2. Ensure folder exists
        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        // 3. Ensure file exists
        try {
            if (!databaseFile.exists()) {
                databaseFile.createNewFile();
            }
        } catch (Exception e) {
            System.out.println("[SQLite] Failed to create database file!");
            e.printStackTrace();
            return;
        }

        // 4. Test connection (important)
        try (Connection ignored = getConnection()) {
            System.out.println("[SQLite] Connection successful: " + databaseFile.getName());
        } catch (SQLException e) {
            System.out.println("[SQLite] Failed to open SQLite connection!");
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        // SQLite uses short-lived connections → nothing to hold open.
    }

    @Override
    public boolean isConnected() {
        // Real check → driver must be loaded AND the DB file must exist
        return driverLoaded && databaseFile.exists();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            throw new SQLException("SQLite driver not loaded! Call connect() first.");
        }

        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }
}
