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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class DBOperator {
    private ConnectorSet connectorSet;
    private HikariDataSource dataSource;
    private Connection rawConnection;
    private String pluginUser;

    public DBOperator(ConnectorSet connectorSet, String pluginUser) {
        this.connectorSet = connectorSet;
        this.pluginUser = pluginUser;

        this.dataSource = buildDataSource();
    }

    public HikariDataSource buildDataSource() {
        HikariConfig config = new HikariConfig();

        switch (connectorSet.getType()) {
            case MYSQL:
                config.setJdbcUrl(connectorSet.getUri());
                config.setUsername(connectorSet.getUsername());
                config.setPassword(connectorSet.getPassword());

                break;
            case SQLITE:
                config.setJdbcUrl(connectorSet.getUri() + getDatabaseFolder().getPath() + File.separator + connectorSet.getSqliteFileName());

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
        return dataSource;
    }

    public Connection buildConnection() {
        try {
            if (rawConnection != null && ! rawConnection.isClosed()) {
                return rawConnection;
            }

            if (dataSource == null) {
                dataSource = buildDataSource();
            }

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

    public ExecutionResult executeSingle(String statement) {
        AtomicReference<ExecutionResult> result = new AtomicReference<>(ExecutionResult.ERROR);

        try {
            Connection connection = getConnection();
            Statement stmt = connection.createStatement();

            if (stmt.execute(statement)) result.set(ExecutionResult.YES);
            else result.set(ExecutionResult.NO);
        } catch (Exception e) {
            MessageUtils.logInfo("Failed to execute statement: " + statement, e);
        }

        return result.get();
    }

    public List<ExecutionResult> execute(String statement) {
        List<ExecutionResult> results = new ArrayList<>();
        String[] statements = statement.split(";");

        for (String s : statements) {
            if (s == null || s.isEmpty() || s.isBlank()) continue;
            String fs = s + ";";
            results.add(executeSingle(fs));
        }

        return results;
    }

    public void executeQuery(String statement, DBAction action) {
        try {
            Connection connection = getConnection();
            Statement stmt = connection.createStatement();
            ResultSet set = stmt.executeQuery(statement);

            action.accept(set);
        } catch (Exception e) {
            MessageUtils.logInfo("Failed to execute query: " + statement, e);
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
        File folder = new File(SLAPI.getInstance().getDataFolder(), "storage");

        if (! folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }
}
