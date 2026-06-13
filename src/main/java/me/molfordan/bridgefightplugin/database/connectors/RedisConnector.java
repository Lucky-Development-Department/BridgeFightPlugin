package me.molfordan.bridgefightplugin.database.connectors;

import me.molfordan.bridgefightplugin.database.RedisDatabaseConnector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisConnector implements RedisDatabaseConnector {

    private final String host;
    private final int port;
    private final String password;
    private final String channel;

    private JedisPool pool;

    public RedisConnector(String host, int port, String password, String channel) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.channel = channel;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void connect() {
        if (pool != null) return;

        if (password == null || password.isEmpty()) {
            pool = new JedisPool(host, port);
        } else {
            pool = new JedisPool(new redis.clients.jedis.JedisPoolConfig(), host, port, 2000, password);
        }
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
