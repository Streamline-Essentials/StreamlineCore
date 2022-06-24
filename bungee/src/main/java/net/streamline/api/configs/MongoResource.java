package net.streamline.api.configs;

import org.bson.Document;

import java.util.TreeMap;

public class MongoResource extends StorageResource<Document> {
    public DatabaseConfig databaseConfig;
    public String collectionName;
    public Document sheet;

    public MongoResource(DatabaseConfig databaseConfig, String collectionName, String discriminatorKey,  Object discriminator) {
        super(Document.class, discriminatorKey, discriminator);
        this.databaseConfig = databaseConfig;
        this.collectionName = collectionName;
        this.sheet = new Document();
    }

    public Document get() {
        return this.databaseConfig.mongoConnection().get(this.collectionName, StorageUtils.getWhere(discriminatorKey, discriminator));
    }

    public void push() {
        this.databaseConfig.mongoConnection().push(this.collectionName, StorageUtils.getWhere(discriminatorKey, discriminator), this.sheet);
    }

    @Override
    public void continueReloadResource() {
        this.sheet = get();
    }

    @Override
    public void write(String key, Object value) {
        this.sheet.put(key, value);

        this.sortDocument();

        this.databaseConfig.mongoConnection().push(this.collectionName, this.getWhere(), this.sheet);
    }

    @Override
    public <O> O getOrSetDefault(String key, O value) {
        O thing = (O) this.sheet.getOrDefault(key, value);
        this.push();
        return thing;
    }

    @Override
    public void sync() {
        this.databaseConfig.mongoConnection().update(this.collectionName, this.getWhere(), this.sheet);
    }

    public Document getWhere() {
        return StorageUtils.getWhere(discriminatorKey, discriminator);
    }

    public void sortDocument() {
        TreeMap<String, Object> toSort = new TreeMap<>(sheet);
        this.sheet = new Document(toSort);
    }
}
