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
        this.sheet = new Document(StorageUtils.parseDotsMongo(discriminatorKey), discriminator);
    }

    public Document get() {
        return this.databaseConfig.mongoConnection().get(this.collectionName, this.getWhere());
    }

    public void push() {
        this.databaseConfig.mongoConnection().push(this.collectionName, this.getWhere(), this.sheet);
    }

    @Override
    public void continueReloadResource() {
        this.sheet = get();
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
        if (this.sheet == null) {
            this.sheet = new Document();
            write(key, value);
        }
        Object get = this.sheet.get(key);
        if (get != null) return (O) get;
        write(key, value);
        O thing = (O) this.sheet.get(key);
        return thing;
    }

    @Override
    public void sync() {
        this.databaseConfig.mongoConnection().push(this.collectionName, this.getWhere(), this.sheet);
    }

    public Document getWhere() {
        return StorageUtils.getWhere(discriminatorKey, discriminator);
    }

    public void sortDocument() {
        TreeMap<String, Object> toSort = new TreeMap<>(sheet);
        this.sheet = new Document(toSort);
    }
}
