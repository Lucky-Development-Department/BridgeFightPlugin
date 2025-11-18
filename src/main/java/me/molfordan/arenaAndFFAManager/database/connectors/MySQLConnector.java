package me.molfordan.arenaAndFFAManager.database.connectors;

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

    private Connection connection;

    public MySQLConnector(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    @Override
    public void connect() {
        if (isConnected()) return;

        try {
            Class.forName("com.mysql.jdbc.Driver"); // MySQL driver for 1.8.8
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("[MySQL] Connected successfully.");

        } catch (Exception e) {
            System.out.println("[MySQL] Failed to connect!");
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        if (!isConnected()) return;

        try {
            connection.close();
            System.out.println("[MySQL] Connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException ignored) {}
        return false;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!isConnected()) connect();
        return connection;
    }
}
