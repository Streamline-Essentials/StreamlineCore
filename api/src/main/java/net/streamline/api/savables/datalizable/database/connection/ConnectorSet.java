package net.streamline.api.savables.datalizable.database.connection;

import lombok.Getter;
import tv.quaint.thebase.lib.leonhard.storage.sections.FlatFileSection;

@Getter
public class ConnectorSet {
    private String databaseName; // Database inside the database.
    private String tablePrefix; // Prefix for all tables in the database.
    private String username; // Username for the database.
    private String password; // Password for the database.
    private String host; // Hostname for the database.
    private int port; // Port for the database.

    private SupportedType type; // Type of database.

    public ConnectorSet() {
        this.databaseName = "database";
        this.tablePrefix = "streamline_";
        this.username = "username";
        this.password = "password";
        this.host = "localhost";
        this.port = 3306;

        this.type = SupportedType.NULL;
    }

    public void read(FlatFileSection section) {
        this.databaseName = section.getString("databaseName");
        this.tablePrefix = section.getString("tablePrefix");
        this.username = section.getString("username");
        this.password = section.getString("password");
        this.host = section.getString("host");
        this.port = section.getInt("port");

        this.type = SupportedType.valueOf(section.getString("type"));
    }

    public ConnectorSet setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public ConnectorSet setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        return this;
    }

    public ConnectorSet setUsername(String username) {
        this.username = username;
        return this;
    }

    public ConnectorSet setPassword(String password) {
        this.password = password;
        return this;
    }

    public ConnectorSet setHost(String host) {
        this.host = host;
        return this;
    }

    public ConnectorSet setPort(int port) {
        this.port = port;
        return this;
    }

    public ConnectorSet setType(SupportedType type) {
        this.type = type;
        return this;
    }

    public enum SupportedType {
        MYSQL,
        SQLITE,
        NULL,
        ;
    }
}
