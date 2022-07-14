package net.streamline.api.configs;

public class MySQLResource extends StorageResource<SQLCollection> {
    public DatabaseConfig databaseConfig;
    public SQLCollection collection;

    public MySQLResource(DatabaseConfig databaseConfig, SQLCollection collection) {
        super(SQLCollection.class, collection.discriminatorKey, collection.discriminator);
        this.databaseConfig = databaseConfig;
        this.collection = collection;
    }

    @Override
    public void continueReloadResource() {
        this.databaseConfig.mySQLConnection().update(this.collection);
    }

    @Override
    public void write(String key, Object value) {
        this.collection.putSet(key, value);
    }

    @Override
    public <O> O getOrSetDefault(String key, O value) {
        return this.collection.getOrSetDefault(key, value);
    }

    @Override
    public void push() {
        this.databaseConfig.mySQLConnection().push(this.collection);
    }

    @Override
    public void delete() {
        this.databaseConfig.mySQLConnection().delete(this.collection);
    }

    @Override
    public boolean exists() {
        return this.databaseConfig.mySQLConnection().exists(this.collection);
    }
}
