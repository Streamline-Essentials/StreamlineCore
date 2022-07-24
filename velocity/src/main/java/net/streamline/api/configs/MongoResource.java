package net.streamline.api.configs;

import org.bson.Document;

import java.util.TreeMap;

public class MongoResource extends StorageResource<Document> {
    public DatabaseConfig databaseConfig;
    public String collectionName;
    public Document sheet = new Document();

    public MongoResource(DatabaseConfig databaseConfig, String collectionName, String discriminatorKey,  Object discriminator) {
        super(Document.class, StorageUtils.parseDotsMongo(discriminatorKey), discriminator);
        this.databaseConfig = databaseConfig;
        this.collectionName = collectionName;
        this.reloadResource(true);
    }

    public Document get() {
        return this.databaseConfig.mongoConnection().get(this.collectionName, this.getWhere());
    }

    public void push() {
        this.databaseConfig.mongoConnection().push(this.collectionName, this.getWhere(), this.sheet);
    }

    @Override
    public void delete() {
        this.databaseConfig.mongoConnection().delete(this.collectionName, this.getWhere());
    }

    @Override
    public boolean exists() {
        return this.databaseConfig.mongoConnection().exists(this.collectionName, this.getWhere());
    }

    @Override
    public <O> O get(String key, Class<O> def) {
        try {
            O object = this.sheet.get(key, def);

            if (! def.isInstance(object)) return null;

            return object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void continueReloadResource() {
        this.sheet = this.get();
        this.map.putAll(this.sheet);
    }

    @Override
    public void write(String key, Object value) {
        key = StorageUtils.parseDotsMongo(key);
        if (this.sheet == null) this.sheet = new Document();

        this.sheet.put(key, value);

        this.sortDocument();
    }

    @Override
    public <O> O getOrSetDefault(String key, O value) {
        key = StorageUtils.parseDotsMongo(key);
        Object t = this.sheet.get(key);
        O thing = this.sheet.get(key, value);
        if (t == null) {
            write(key, thing);
        }
        return thing;
    }

    public Document getWhere() {
        return StorageUtils.getWhere(discriminatorKey, discriminator);
    }

    public void sortDocument() {
        TreeMap<String, Object> toSort = new TreeMap<>(sheet);
        this.sheet = new Document(toSort);
    }
}
