package net.streamline.api.savables.datalizable.database;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.savables.datalizable.database.connection.ConnectorSet;
import net.streamline.api.savables.datalizable.database.connection.IConnector;
import net.streamline.api.savables.datalizable.database.connection.SQLiteConnector;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

public class DatabaseManager {
    @Getter @Setter
    public static class DatabaseConfig extends SimpleConfiguration {
        public DatabaseConfig() {
            super("database.yml", SLAPI.getBaseModule(), false);
        }

        @Override
        public void init() {
            getHost();
            getPort();
            getUsername();
            getPassword();
            getDatabaseName();
            getTablePrefix();
            getDatabaseType();
        }

        public String getHost() {
            return getOrSetDefault("host", "localhost");
        }

        public int getPort() {
            return getOrSetDefault("port", 3306);
        }

        public String getUsername() {
            return getOrSetDefault("username", "username");
        }

        public String getPassword() {
            return getOrSetDefault("password", "password");
        }

        public String getDatabaseName() {
            return getOrSetDefault("databaseName", "database");
        }

        public String getTablePrefix() {
            return getOrSetDefault("tablePrefix", "streamline_");
        }

        public ConnectorSet.SupportedType getDatabaseType() {
            return ConnectorSet.SupportedType.valueOf(getOrSetDefault("type", "SQLITE"));
        }
    }

    @Getter @Setter
    private static IConnector<?> connector;
    @Getter @Setter
    private static DatabaseConfig config;

    public static void init() {
        config = new DatabaseConfig();

        ConnectorSet connSet = new ConnectorSet();
        connSet.setDatabaseName(config.getDatabaseName());
        connSet.setHost(config.getHost());
        connSet.setPassword(config.getPassword());
        connSet.setPort(config.getPort());
        connSet.setTablePrefix(config.getTablePrefix());
        connSet.setType(config.getDatabaseType());
        connSet.setUsername(config.getUsername());

        switch (connSet.getType()) {
            case SQLITE:
                connector = new SQLiteConnector(connSet);
                break;
            case MYSQL:
                // not implemented yet
                return;
            default:
                return;
        }
    }

    public static void initTables() {
        initUserTable();
    }

    public static void initUserTable() {

    }
}
