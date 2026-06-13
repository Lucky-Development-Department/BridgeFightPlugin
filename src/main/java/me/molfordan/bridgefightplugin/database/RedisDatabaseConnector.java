package me.molfordan.bridgefightplugin.database;

import redis.clients.jedis.Jedis;

public interface RedisDatabaseConnector extends DatabaseConnector {
    Jedis getRedisClient();
    String getChannel();
}
