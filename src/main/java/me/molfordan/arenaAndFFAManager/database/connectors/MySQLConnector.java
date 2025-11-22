package me.molfordan.arenaAndFFAManager.database.connectors;

import me.molfordan.arenaAndFFAManager.ArenaAndFFAManager;
import me.molfordan.arenaAndFFAManager.database.SQLDatabaseConnector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnector implements SQLDatabaseConnector {

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    // Do NOT store a persistent connection
    private final ArenaAndFFAManager plugin;

    public MySQLConnector(String host, int port, String database, String user, String password, ArenaAndFFAManager plugin) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        // Test connection once on startup
        try (Connection test = createNewConnection()) {
            plugin.debug("[MySQL] Connected successfully.");
        } catch (Exception e) {
            plugin.debug("[MySQL] Failed to connect!");
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        // Nothing to close because we never hold long-lived connections
    }

    @Override
    public boolean isConnected() {
        try (Connection test = createNewConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Always returns a FRESH connection.
     * This completely avoids stale / broken connection problems.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return createNewConnection();
    }

    private Connection createNewConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}

        String url =
                "jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        return DriverManager.getConnection(url, user, password);
    }
}
