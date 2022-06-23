package net.streamline.api.base.configs;

public class DatabaseConfig {
    public String connectionUri;
    public String database;
    public String prefix;
    public StorageUtils.DatabaseType type;

    public DatabaseConfig(String connectionUri, String database, String prefix, StorageUtils.DatabaseType type) {
        this.connectionUri = connectionUri;
        this.database = database;
        this.prefix = prefix;
    }

    public MongoConnection mongoConnection() {
        return new MongoConnection(this.connectionUri, this.database);
    }
}
