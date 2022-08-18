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
    public String tablePrefix;

    public MongoConnection(String connectionUri, String database, String tablePrefix) {
        this.client = new MongoClient(new MongoClientURI(connectionUri));
        this.database = client.getDatabase(database);
        this.tablePrefix = tablePrefix;
    }

    public List<String> listCollections() {
        List<String> collections = new ArrayList<>();
        collections = this.database.listCollectionNames().into(collections);
        return collections;
    }

    public String getPrefixed(String collection) {
        return this.tablePrefix + collection;
    }

    public boolean hasCollection(String name) {
        return listCollections().contains(name);
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        if (! hasCollection(getPrefixed(collectionName))) this.database.createCollection(getPrefixed(collectionName));
        return this.database.getCollection(getPrefixed(collectionName));
    }

    public boolean exists(String collectionName, Document check) {
        return getCollection(collectionName).find(check).first() != null;
    }

    public void push(String collectionName, Document where, Document toPush) {
        toPush.remove("_id");
        if (exists(collectionName, where)) {
            update(collectionName, where, toPush);
        } else {
            insert(collectionName, toPush);
        }
    }

    public void update(String collectionName, Document where, Document toUpdate) {
        try {
            Document update = new Document("$set", toUpdate);
            getCollection(collectionName).updateOne(where, update);
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    public void replaceOne(String collectionName, Document where, Document toUpdate) {
        try {
            getCollection(collectionName).replaceOne(where, toUpdate);
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    public void insert(String collectionName, Document toInsert) {
        try {
            getCollection(collectionName).insertOne(toInsert);
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    public void delete(String collectionName, Document where) {
        getCollection(collectionName).deleteOne(where);
    }

    public Document get(String collectionName, Document where) {
        return getCollection(collectionName).find(where).first();
    }
}
