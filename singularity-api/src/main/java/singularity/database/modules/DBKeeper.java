package singularity.database.modules;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.CoreDBOperator;
import singularity.database.DatabaseType;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract, database-type-aware persistence manager for a single class of
 * {@link Identifiable} resources.
 *
 * <p>Concrete subclasses handle the actual SQL statements by implementing the
 * {@code *Mysql} and {@code *Sqlite} method pairs for save, load, and exists
 * operations. {@code DBKeeper} itself delegates to the correct pair at runtime
 * based on the active {@link DatabaseType}.</p>
 *
 * <p>A {@link ResourceGetter} is provided at construction time and used as a
 * fallback factory when neither MySQL nor SQLite paths apply (or for default
 * object creation).</p>
 *
 * @param <T> the type of resource managed by this keeper; must implement
 *            {@link Identifiable}
 */
@Getter
@Setter
public abstract class DBKeeper<T extends Identifiable> implements Identifiable {

    /**
     * The unique identifier string that names this keeper and is used as a
     * table/resource label.
     */
    private String identifier;

    /**
     * A factory function that constructs a default instance of {@code T} from an
     * identifier string, used when the database returns no result.
     */
    private ResourceGetter<T> getter;

    /**
     * Creates a {@code DBKeeper} with the given identifier and resource factory.
     *
     * @param identifier the unique name for this keeper
     * @param getter     the factory used to create default resource instances
     */
    public DBKeeper(String identifier, ResourceGetter<T> getter) {
        this.identifier = identifier;
        this.getter = getter;
    }

    /**
     * Returns the shared {@link CoreDBOperator} instance from the Singularity
     * framework.
     *
     * @return the main database operator
     */
    public static CoreDBOperator getDatabase() {
        return Singularity.getMainDatabase();
    }

    /**
     * Returns the {@link DatabaseType} currently configured for the main database.
     *
     * @return the active database type (MySQL or SQLite)
     */
    public static DatabaseType getDatabaseType() {
        return getDatabase().getConnectorSet().getType();
    }

    /**
     * Ensures the tables required by this keeper exist in the database, delegating
     * to {@link #ensureMysqlTables()} or {@link #ensureSqliteTables()} based on the
     * active database type.
     */
    public void ensureTables() {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            ensureMysqlTables();
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            ensureSqliteTables();
        }
    }

    /**
     * Creates any MySQL-specific tables required by this keeper. Called by
     * {@link #ensureTables()} when the active database type is
     * {@link DatabaseType#MYSQL}.
     */
    public abstract void ensureMysqlTables();

    /**
     * Creates any SQLite-specific tables required by this keeper. Called by
     * {@link #ensureTables()} when the active database type is
     * {@link DatabaseType#SQLITE}.
     */
    public abstract void ensureSqliteTables();

    /**
     * Persists the given resource object to the database.
     *
     * @param obj   the resource to save
     * @param async if {@code true} the save is performed on a background thread;
     *              if {@code false} the calling thread blocks until completion
     */
    public void save(T obj, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> saveRaw(obj));
        } else {
            saveRaw(obj);
        }
    }

    /**
     * Asynchronously persists the given resource object to the database. Equivalent
     * to {@link #save(Identifiable, boolean)} with {@code async = true}.
     *
     * @param obj the resource to save
     */
    public void save(T obj) {
        save(obj, true);
    }

    /**
     * Synchronously persists the given resource, ensuring the required tables exist
     * first, then delegating to {@link #saveMysql(Identifiable)} or
     * {@link #saveSqlite(Identifiable)} based on the active database type.
     *
     * @param obj the resource to save
     */
    public void saveRaw(T obj) {
        ensureTables();

        if (getDatabaseType() == DatabaseType.MYSQL) {
            saveMysql(obj);
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            saveSqlite(obj);
        }
    }

    /**
     * Returns the table prefix configured for the main database connection.
     *
     * @return the table prefix string (may be empty but never {@code null})
     */
    public String getTablePrefix() {
        return getDatabase().getConnectorSet().getTablePrefix();
    }

    /**
     * Replaces the {@code %table_prefix%} placeholder within a SQL statement string
     * with the actual table prefix.
     *
     * @param statement the SQL statement containing the placeholder
     * @return the statement with {@code %table_prefix%} substituted
     */
    public String injectTablePrefix(String statement) {
        return statement.replace("%table_prefix%", getTablePrefix());
    }

    /**
     * Returns the database name (schema) configured for the main database connection.
     *
     * @return the database name string
     */
    public String getDatabaseName() {
        return getDatabase().getConnectorSet().getDatabase();
    }

    /**
     * Replaces the {@code %database_name%} placeholder within a SQL statement string
     * with the actual database name.
     *
     * @param statement the SQL statement containing the placeholder
     * @return the statement with {@code %database_name%} substituted
     */
    public String injectDatabaseName(String statement) {
        return statement.replace("%database_name%", getDatabaseName());
    }

    /**
     * Saves the resource using MySQL-specific SQL. Called by {@link #saveRaw(Identifiable)}
     * when the active database type is {@link DatabaseType#MYSQL}.
     *
     * @param obj the resource to persist
     */
    public abstract void saveMysql(T obj);

    /**
     * Saves the resource using SQLite-specific SQL. Called by {@link #saveRaw(Identifiable)}
     * when the active database type is {@link DatabaseType#SQLITE}.
     *
     * @param obj the resource to persist
     */
    public abstract void saveSqlite(T obj);

    /**
     * Asynchronously loads a resource by its identifier string.
     *
     * @param identifier the identifier of the resource to load
     * @return a {@link CompletableFuture} resolving to an {@link Optional} containing
     *         the resource, or empty if it was not found
     */
    public CompletableFuture<Optional<T>> load(String identifier) {
        return CompletableFuture.supplyAsync(() -> loadRaw(identifier));
    }

    /**
     * Synchronously loads a resource by its identifier string, ensuring tables exist
     * first and delegating to {@link #loadMysql(String)} or
     * {@link #loadSqlite(String)} based on the active database type.
     *
     * <p>Falls back to {@link ResourceGetter#apply(Object)} when neither MySQL nor
     * SQLite paths apply.</p>
     *
     * @param identifier the identifier of the resource to load
     * @return an {@link Optional} containing the loaded resource, or the getter's
     *         default value wrapped in an {@link Optional}
     */
    public Optional<T> loadRaw(String identifier) {
        ensureTables();

        if (getDatabaseType() == DatabaseType.MYSQL) {
            return loadMysql(identifier);
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            return loadSqlite(identifier);
        }
        return Optional.of(getGetter().apply(identifier));
    }

    /**
     * Loads the resource using MySQL-specific SQL. Called by {@link #loadRaw(String)}
     * when the active database type is {@link DatabaseType#MYSQL}.
     *
     * @param identifier the identifier of the resource to load
     * @return an {@link Optional} containing the resource, or empty if not found
     */
    public abstract Optional<T> loadMysql(String identifier);

    /**
     * Loads the resource using SQLite-specific SQL. Called by {@link #loadRaw(String)}
     * when the active database type is {@link DatabaseType#SQLITE}.
     *
     * @param identifier the identifier of the resource to load
     * @return an {@link Optional} containing the resource, or empty if not found
     */
    public abstract Optional<T> loadSqlite(String identifier);

    /**
     * Synchronously checks whether a resource with the given identifier exists in the
     * database, blocking the calling thread until the result is available.
     *
     * @param identifier the identifier to check
     * @return {@code true} if the resource exists; {@code false} otherwise
     */
    public boolean forceExists(String identifier) {
        return exists(identifier).join();
    }

    /**
     * Asynchronously checks whether a resource with the given identifier exists in the
     * database.
     *
     * @param identifier the identifier to check
     * @return a {@link CompletableFuture} resolving to {@code true} if the resource
     *         exists, or {@code false} otherwise
     */
    public CompletableFuture<Boolean> exists(String identifier) {
        return CompletableFuture.supplyAsync(() -> existsRaw(identifier));
    }

    /**
     * Synchronously checks existence, ensuring tables are present first and
     * delegating to {@link #existsMysql(String)} or {@link #existsSqlite(String)}.
     *
     * @param identifier the identifier to check
     * @return {@code true} if the resource exists; {@code false} if not found or if
     *         neither MySQL nor SQLite paths apply
     */
    public boolean existsRaw(String identifier) {
        ensureTables();

        if (getDatabaseType() == DatabaseType.MYSQL) {
            return existsMysql(identifier);
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            return existsSqlite(identifier);
        }
        return false;
    }

    /**
     * Checks existence using MySQL-specific SQL. Called by {@link #existsRaw(String)}
     * when the active database type is {@link DatabaseType#MYSQL}.
     *
     * @param identifier the identifier to check
     * @return {@code true} if a matching row exists; {@code false} otherwise
     */
    public abstract boolean existsMysql(String identifier);

    /**
     * Checks existence using SQLite-specific SQL. Called by {@link #existsRaw(String)}
     * when the active database type is {@link DatabaseType#SQLITE}.
     *
     * @param identifier the identifier to check
     * @return {@code true} if a matching row exists; {@code false} otherwise
     */
    public abstract boolean existsSqlite(String identifier);
}
