package net.streamline.api.database;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.utils.MessageUtils;
import tv.quaint.thebase.lib.hikari.HikariConfig;
import tv.quaint.thebase.lib.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Getter @Setter
public class DBOperator {
    private ConnectorSet connectorSet;
    private HikariDataSource dataSource;
    private Connection rawConnection;
    private String pluginUser;

    public DBOperator(ConnectorSet connectorSet, String pluginUser) {
        this.connectorSet = connectorSet;
        this.pluginUser = pluginUser;
    }

    public Connection buildConnection() {
        try {
            if (rawConnection != null && ! rawConnection.isClosed()) {
                return rawConnection;
            }

            HikariConfig config = new HikariConfig();

            switch (connectorSet.getType()) {
                case MYSQL:
                    config.setJdbcUrl(connectorSet.getUri());
                    config.setUsername(connectorSet.getUsername());
                    config.setPassword(connectorSet.getPassword());

                    break;
                case SQLITE:
                    config.setJdbcUrl(connectorSet.getUri());

                    break;
            }
            config.setPoolName(pluginUser + " - Pool");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setDriverClassName(connectorSet.getType().getDriver());
            config.addDataSourceProperty("allowMultiQueries", true);

            dataSource = new HikariDataSource(config);

            rawConnection = dataSource.getConnection();
            return rawConnection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Connection getConnection() {
        try {
            if (rawConnection != null && ! rawConnection.isClosed()) {
                return rawConnection;
            }

            return buildConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean execute(String statement) {
        try (Connection connection = getConnection()) {
            Statement stmt = connection.createStatement();
            return stmt.execute(statement);
        } catch (Exception e) {
            MessageUtils.logInfo("Failed to execute statement: " + statement, e);
            return false;
        }
    }

    public ResultSet executeQuery(String statement) {
        try (Connection connection = getConnection()) {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(statement);
        } catch (Exception e) {
            MessageUtils.logInfo("Failed to execute query: " + statement, e);
            return null;
        }
    }

    public void createSqliteFileIfNotExists() {
        if (connectorSet.getType() != DatabaseType.SQLITE) return;

        File file = new File(getDatabaseFolder(), connectorSet.getSqliteFileName());
        if (! file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void ensureFile() {
        if (this.getConnectorSet().getType() != DatabaseType.SQLITE) return;

        String s1 = this.getConnectorSet().getSqliteFileName();
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        createSqliteFileIfNotExists();
    }

    public static File getDatabaseFolder() {
        File folder = new File(SLAPI.getMainFolder(), "storage");

        if (! folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }
}
