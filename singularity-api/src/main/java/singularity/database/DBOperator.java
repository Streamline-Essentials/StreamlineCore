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

/**
 * Abstract base class for HikariCP-backed JDBC database operators.
 *
 * <p>{@code DBOperator} manages a {@link HikariDataSource} connection pool and
 * exposes a small set of helper methods for executing SQL statements and queries.
 * Concrete subclasses (e.g. {@link CoreDBOperator}) are responsible for
 * implementing schema creation via {@link #ensureDatabase()} and
 * {@link #ensureTables()}, then call {@link #ensureUsable()} before performing
 * any data access.</p>
 *
 * <p>Both MySQL and SQLite are supported via the configured {@link ConnectorSet}.
 * For SQLite the database file is created automatically inside the plugin's
 * {@code storage/} sub-directory.</p>
 */
@Getter @Setter
public abstract class DBOperator {

    /** Connection and authentication details for the underlying database. */
    private ConnectorSet connectorSet;

    /** The active HikariCP connection pool managed by this operator. */
    private HikariDataSource dataSource;

    /**
     * A human-readable label identifying the plugin or component that owns this
     * operator, used as the HikariCP pool name suffix.
     */
    private String pluginUser;

    /**
     * A raw JDBC connection retained for diagnostic or legacy use. Not part of the
     * pooled connection lifecycle.
     */
    private Connection rawConnection;

    /**
     * Constructs a new {@code DBOperator}, immediately building and opening the
     * HikariCP connection pool.
     *
     * @param connectorSet the database connection configuration
     * @param pluginUser   a label used to identify this pool in logs and pool names
     */
    public DBOperator(ConnectorSet connectorSet, String pluginUser) {
        this.connectorSet = connectorSet;
        this.pluginUser = pluginUser;

        this.dataSource = buildDataSource();
    }

    /**
     * Constructs and configures a {@link HikariDataSource} from the current
     * {@link ConnectorSet}.
     *
     * <p>For MySQL the URL, username, and password are applied directly. For SQLite
     * the database file is created if it does not yet exist and the full file path
     * is appended to the JDBC URL prefix.</p>
     *
     * @return a newly created and configured {@link HikariDataSource}
     */
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

    /**
     * Borrows a {@link Connection} from the HikariCP pool. If the pool has not been
     * initialised yet it is created on demand.
     *
     * @return a valid pooled connection, or {@code null} if the pool could not
     *         provide one
     */
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

    /**
     * Returns the {@link DatabaseType} configured for this operator.
     *
     * @return the active database type (MySQL or SQLite)
     */
    public DatabaseType getType() {
        return connectorSet.getType();
    }

    /**
     * Executes a single SQL statement, applying parameter bindings via
     * {@code statementBuilder} before running it.
     *
     * @param statement        the SQL statement to execute (must not contain
     *                         {@code ;;} separators)
     * @param statementBuilder a consumer that binds parameters onto the
     *                         {@link PreparedStatement}
     * @return {@link ExecutionResult#YES} if the statement returned a result set,
     *         {@link ExecutionResult#NO} if it did not, or
     *         {@link ExecutionResult#ERROR} if an exception was thrown
     */
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

    /**
     * Executes a SQL query and processes the resulting {@link ResultSet} with the
     * provided {@link DBAction}.
     *
     * @param statement        the SQL SELECT statement to execute
     * @param statementBuilder a consumer that binds parameters onto the
     *                         {@link PreparedStatement}
     * @param action           the {@link DBAction} that processes the returned
     *                         {@link ResultSet}
     */
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

    /**
     * Executes one or more SQL statements separated by {@code ;;} within a single
     * string. Each sub-statement is executed individually via
     * {@link #executeSingle(String, Consumer)}.
     *
     * @param statement        one or more SQL statements delimited by {@code ;;}
     * @param statementBuilder a consumer that binds parameters onto each
     *                         {@link PreparedStatement}; note that the same consumer
     *                         is applied to every sub-statement
     * @return a list of {@link ExecutionResult} values, one per sub-statement
     *         executed
     */
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

    /**
     * Ensures the SQLite database file exists on disk. This is a no-op for
     * non-SQLite configurations.
     */
    public void ensureFile() {
        if (this.getConnectorSet().getType() != DatabaseType.SQLITE) return;

        String s1 = this.getConnectorSet().getSqliteFileName();
        if (s1 == null) return;
        if (s1.isBlank()) return;

        initializeSQLiteDatabase();
    }

    /**
     * Ensures that the target database (schema) exists. Called by
     * {@link #ensureUsable()} before table creation. Implementations should execute
     * a {@code CREATE DATABASE IF NOT EXISTS} statement or equivalent.
     */
    public abstract void ensureTables();

    /**
     * Ensures that all required tables exist in the database. Called by
     * {@link #ensureUsable()} after database creation. Implementations should execute
     * {@code CREATE TABLE IF NOT EXISTS} statements for every table they use.
     */
    public abstract void ensureDatabase();

    /**
     * Convenience method that sequentially calls {@link #ensureFile()},
     * {@link #ensureDatabase()}, and {@link #ensureTables()} to guarantee the
     * database is ready for use before any data operations.
     */
    public void ensureUsable() {
        this.ensureFile();
        this.ensureDatabase();
        this.ensureTables();
    }

    /**
     * Returns the {@code storage/} sub-directory inside the plugin's data folder,
     * creating it if it does not yet exist.
     *
     * @return the {@link File} representing the storage directory
     */
    public static File getDatabaseFolder() {
        File folder = new File(Singularity.getInstance().getDataFolder(), "storage");

        if (! folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    /**
     * Creates the SQLite database file on disk if it does not yet exist. Only
     * executes when the configured {@link DatabaseType} is {@link DatabaseType#SQLITE}.
     */
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
