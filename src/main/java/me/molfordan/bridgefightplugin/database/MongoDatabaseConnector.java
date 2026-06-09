package me.molfordan.bridgefightplugin.database;


import com.mongodb.client.MongoDatabase;

public interface MongoDatabaseConnector extends DatabaseConnector {
    MongoDatabase getMongoDatabase();
}
