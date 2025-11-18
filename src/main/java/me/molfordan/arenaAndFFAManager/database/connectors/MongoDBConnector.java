package me.molfordan.arenaAndFFAManager.database.connectors;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import me.molfordan.arenaAndFFAManager.database.MongoDatabaseConnector;

public class MongoDBConnector implements MongoDatabaseConnector {

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private MongoClient client;
    private MongoDatabase mongoDb;

    public MongoDBConnector(String host, int port, String database, String user, String password) {
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
            String uri;

            if (user != null && !user.isEmpty()) {
                uri = "mongodb://" + user + ":" + password + "@" + host + ":" + port + "/" + database;
            } else {
                uri = "mongodb://" + host + ":" + port + "/" + database;
            }

            client = MongoClients.create(uri);
            mongoDb = client.getDatabase(database);

            System.out.println("[MongoDB] Connected.");
        } catch (Exception ex) {
            System.out.println("[MongoDB] Failed to connect!");
            ex.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
            mongoDb = null;
            System.out.println("[MongoDB] Disconnected.");
        }
    }

    @Override
    public boolean isConnected() {
        return client != null;
    }

    @Override
    public MongoDatabase getMongoDatabase() {
        return mongoDb;
    }
}
