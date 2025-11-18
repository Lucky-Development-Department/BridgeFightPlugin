package me.molfordan.arenaAndFFAManager.database;

import redis.clients.jedis.Jedis;

public interface RedisDatabaseConnector extends DatabaseConnector {
    Jedis getRedisClient();
}
