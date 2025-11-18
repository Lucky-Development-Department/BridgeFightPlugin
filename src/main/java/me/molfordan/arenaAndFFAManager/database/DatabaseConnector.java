

package me.molfordan.arenaAndFFAManager.database;

import com.mongodb.client.MongoDatabase;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnector {

    void connect();
    void disconnect();
    boolean isConnected();

    // --- SQL ---
    default Connection getSQLConnection() throws SQLException {
        return null;
    }

    // --- Redis ---
    default Jedis getRedisClient() {
        return null;
    }

    // --- MongoDB ---
    default MongoDatabase getMongoDatabase() {
        return null;
    }
}

