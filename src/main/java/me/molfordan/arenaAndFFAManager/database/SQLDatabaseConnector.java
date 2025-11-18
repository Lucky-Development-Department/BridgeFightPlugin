package me.molfordan.arenaAndFFAManager.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLDatabaseConnector extends DatabaseConnector {
    Connection getConnection() throws SQLException;
}
