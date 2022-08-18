package net.streamline.api.configs;

public class DatabaseConfig {
    public String connectionUri;
    public String database;
    public String prefix;
    public StorageUtils.DatabaseType type;
    private MongoConnection mongoConnection;
    private MySQLConnection mySQLConnection;

    public DatabaseConfig(String connectionUri, String database, String prefix, StorageUtils.DatabaseType type) {
        this.connectionUri = connectionUri;
        this.database = database;
        this.prefix = prefix;
        this.type = type;
    }

    public MongoConnection mongoConnection() {
        if (this.mongoConnection == null) {
            this.mongoConnection = new MongoConnection(this.connectionUri, this.database, this.prefix);
        }
        return this.mongoConnection;
    }

    public MySQLConnection mySQLConnection() {
        if (this.mySQLConnection == null) {
            this.mySQLConnection = new MySQLConnection(this.connectionUri, this.database, this.prefix);
        }
        return this.mySQLConnection;
    }
}
