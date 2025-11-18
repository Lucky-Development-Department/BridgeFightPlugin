package me.molfordan.arenaAndFFAManager.database;


import com.mongodb.client.MongoDatabase;

public interface MongoDatabaseConnector extends DatabaseConnector {
    MongoDatabase getMongoDatabase();
}
