package net.streamline.api.configs;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoConnection {
    public MongoClient client;
    public MongoDatabase database;

    public MongoConnection(String connectionUri, String database) {
        this.client = new MongoClient(new MongoClientURI(connectionUri));
        this.database = client.getDatabase(database);
    }

    public List<String> listCollections() {
        List<String> collections = new ArrayList<>();
        collections = this.database.listCollectionNames().into(collections);
        return collections;
    }

    public boolean hasCollection(String name) {
        return listCollections().contains(name);
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        if (! hasCollection(collectionName)) this.database.createCollection(collectionName);
        return this.database.getCollection(collectionName);
    }

    public boolean exists(String collectionName, Document check) {
        return getCollection(collectionName).find(check).first() != null;
    }

    public void push(String collectionName, Document where, Document toPush) {
        if (exists(collectionName, where)) {
            update(collectionName, where, toPush);
        } else {
            insert(collectionName, toPush);
        }
    }

    public void update(String collectionName, Document where, Document toUpdate) {
        getCollection(collectionName).updateOne(where, toUpdate);
    }

    public void replaceOne(String collectionName, Document where, Document toUpdate) {
        getCollection(collectionName).replaceOne(where, toUpdate);
    }

    public void insert(String collectionName, Document toInsert) {
        getCollection(collectionName).insertOne(toInsert);
    }

    public void delete(String collectionName, Document where) {
        getCollection(collectionName).deleteOne(where);
    }

    public Document get(String collectionName, Document where) {
        return getCollection(collectionName).find(where).first();
    }
}
