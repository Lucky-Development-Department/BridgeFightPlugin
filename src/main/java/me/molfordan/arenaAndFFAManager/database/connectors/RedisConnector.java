package me.molfordan.arenaAndFFAManager.database.connectors;

import me.molfordan.arenaAndFFAManager.database.RedisDatabaseConnector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisConnector implements RedisDatabaseConnector {

    private final String host;
    private final int port;

    private JedisPool pool;

    public RedisConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect() {
        if (pool != null) return;

        pool = new JedisPool(host, port);
        System.out.println("[Redis] JedisPool initialized.");
    }

    @Override
    public void disconnect() {
        if (pool != null) {
            pool.close();
            pool = null;
            System.out.println("[Redis] Connection closed.");
        }
    }

    @Override
    public boolean isConnected() {
        return pool != null;
    }

    @Override
    public Jedis getRedisClient() {
        return pool.getResource();
    }
}
