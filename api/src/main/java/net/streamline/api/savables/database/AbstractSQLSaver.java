package net.streamline.api.savables.database;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.utils.MessageUtils;
import tv.quaint.thebase.lib.hikari.HikariConfig;
import tv.quaint.thebase.lib.hikari.HikariDataSource;

import java.sql.Connection;

@Getter @Setter
public class AbstractSQLSaver {
    private HikariDataSource dataSource;
    private Connection rawConnection;
    private ConnectionSet connectionSet;

    public AbstractSQLSaver(ConnectionSet connectionSet) {
        this.connectionSet = connectionSet;
    }

    public Connection buildNewConnection() {
        try {
            if (rawConnection != null && !rawConnection.isClosed()) {
                return rawConnection;
            }

            switch (getConnectionSet().getType()) {
                case MYSQL:
                    HikariConfig mysqlConfig = new HikariConfig();

                    mysqlConfig.setJdbcUrl(getConnectionSet().getUrl());
                    mysqlConfig.setUsername(getConnectionSet().getUsername());
                    mysqlConfig.setPassword(getConnectionSet().getPassword());
                    mysqlConfig.setConnectionTimeout(10000);
                    mysqlConfig.setLeakDetectionThreshold(10000);
                    mysqlConfig.setMaximumPoolSize(10);
                    mysqlConfig.setMinimumIdle(5);
                    mysqlConfig.setMaxLifetime(60000);
                    mysqlConfig.setPoolName("Streamline");
                    mysqlConfig.addDataSourceProperty("allowMultiQueries", true); // Allows multiple queries to be executed in one statement
                    mysqlConfig.setDriverClassName(getConnectionSet().getDriver());

                    setDataSource(new HikariDataSource(mysqlConfig));
                    break;
                case SQLITE:
                    HikariConfig sqliteConfig = new HikariConfig();

                    sqliteConfig.setJdbcUrl(getConnectionSet().getUrl());
                    sqliteConfig.setConnectionTimeout(10000);
                    sqliteConfig.setLeakDetectionThreshold(10000);
                    sqliteConfig.setMaximumPoolSize(10);
                    sqliteConfig.setMinimumIdle(5);
                    sqliteConfig.setMaxLifetime(60000);
                    sqliteConfig.setPoolName("Streamline");
                    sqliteConfig.addDataSourceProperty("allowMultiQueries", true); // Allows multiple queries to be executed in one statement
                    sqliteConfig.setDriverClassName(getConnectionSet().getDriver());

                    setDataSource(new HikariDataSource(sqliteConfig));
                    break;
            }

            return getDataSource().getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Connection getConnection() {
        try {
            if (rawConnection == null) {
                rawConnection = buildNewConnection();
                return rawConnection;
            }
            if (! rawConnection.isClosed()) {
                return rawConnection;
            } else {
                rawConnection = buildNewConnection();
                return rawConnection;
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Could not connect to the database!", e);
            return null;
        }
    }
}
