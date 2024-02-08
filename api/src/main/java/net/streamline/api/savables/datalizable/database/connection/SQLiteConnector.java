package net.streamline.api.savables.datalizable.database.connection;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.utils.MessageUtils;
import tv.quaint.thebase.lib.hikari.HikariConfig;

import java.sql.Connection;

@Getter @Setter
public class SQLiteConnector extends SQLConnector {
    private HikariConfig config;

    public SQLiteConnector(ConnectorSet connectorSet) {
        super(connectorSet);
    }

    @Override
    public Connection buildConnection() {
        try {
            if (config == null) {
                config = new HikariConfig();
                Class.forName("org.sqlite.JDBC");

                config.setJdbcUrl("jdbc:sqlite:" + getConnectorSet().getDatabaseName() + ".db");
                config.setDriverClassName("org.sqlite.JDBC");
            }

            return config.getDataSource().getConnection();
        } catch (Exception e) {
            MessageUtils.logSevereWithInfo("Failed to connect to SQLite database.", e);
        }

        return null;
    }
}
