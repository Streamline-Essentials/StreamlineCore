package singularity.database;

import gg.drak.thebase.lib.hikari.HikariConfig;
import gg.drak.thebase.lib.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.utils.MessageUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter @Setter
public abstract class DBOperator {
    private ConnectorSet connectorSet;
    private HikariDataSource dataSource;
    private String pluginUser;

    private Connection rawConnection;

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
                initializeSQLiteDatabase();

                config.setJdbcUrl(connectorSet.getUri() + getDatabaseFolder().getPath() + File.separator + connectorSet.getSqliteFileName());

                break;
        }
        config.setPoolName(pluginUser + " - Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setDriverClassName(connectorSet.getType().getDriver());
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    public Connection getConnection() {
        try {
            if (dataSource == null) {
                dataSource = buildDataSource();
            }
            return dataSource.getConnection();
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to get connection", e);
            return null;
        }
    }

    public DatabaseType getType() {
        return connectorSet.getType();
    }

    public ExecutionResult executeSingle(String statement, Consumer<PreparedStatement> statementBuilder) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(statement)) {

            statementBuilder.accept(stmt);

            return stmt.execute() ? ExecutionResult.YES : ExecutionResult.NO;
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to execute statement: " + statement, e);
            return ExecutionResult.ERROR;
        }
    }

    public void executeQuery(String statement, Consumer<PreparedStatement> statementBuilder, DBAction action) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(statement)) {

            statementBuilder.accept(stmt);
            try (ResultSet set = stmt.executeQuery()) {
                action.accept(set);
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to execute query: " + statement, e);
        }
    }

    public List<ExecutionResult> execute(String statement, Consumer<PreparedStatement> statementBuilder) {
        List<ExecutionResult> results = new ArrayList<>();

        String[] statements = statement.split(";;");

        for (String s : statements) {
            if (s == null || s.isEmpty() || s.isBlank()) continue;
            String fs = s;
            if (! fs.endsWith(";")) fs += ";";
            results.add(executeSingle(fs, statementBuilder));
        }

        return results;
    }

    public void ensureFile() {
        if (this.getConnectorSet().getType() != DatabaseType.SQLITE) return;

        String s1 = this.getConnectorSet().getSqliteFileName();
        if (s1 == null) return;
        if (s1.isBlank()) return;

        initializeSQLiteDatabase();
    }

    public abstract void ensureTables();

    public abstract void ensureDatabase();

    public void ensureUsable() {
        this.ensureFile();
        this.ensureDatabase();
        this.ensureTables();
    }

    public static File getDatabaseFolder() {
        File folder = new File(Singularity.getInstance().getDataFolder(), "storage");

        if (! folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    private void initializeSQLiteDatabase() {
        if (connectorSet.getType() == DatabaseType.SQLITE) {
            File file = new File(getDatabaseFolder(), connectorSet.getSqliteFileName());
            if (! file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to create SQLite database file", e);
                }
            }
        }
    }
}
