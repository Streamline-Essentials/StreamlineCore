package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.ConnectorSet;
import singularity.database.DatabaseType;

/**
 * Configuration handler for database connection settings, backed by
 * {@code database-config.yml} in the plugin's data folder.
 *
 * <p>Supports all {@link DatabaseType} backends. When the type is
 * {@link DatabaseType#SQLITE}, only the SQLite file name is relevant;
 * host/port/username/password are used for remote engines such as MySQL.</p>
 *
 * <p>Calling {@link #init()} triggers each getter so that all keys are
 * written with their defaults on first load.</p>
 */
@Getter @Setter
public class DatabaseConfigHandler extends SimpleConfiguration {

    /**
     * Constructs the handler, loading (or creating) {@code database-config.yml}
     * from the Singularity plugin's data folder as a self-contained resource.
     */
    public DatabaseConfigHandler() {
        super("database-config.yml", Singularity.getInstance(), true);
    }

    /**
     * Eagerly reads every database setting so that default values are written
     * to the file when it is first created.
     */
    @Override
    public void init() {
        getDatabaseHost();
        getDatabasePort();
        getDatabaseUsername();
        getDatabasePassword();
        getDatabaseTablePrefix();
        getDatabaseName();
        getDatabaseType();
        getSqliteFileName();
    }

    /**
     * Returns the database server hostname, defaulting to {@code "localhost"}.
     * Reloads the resource before reading.
     *
     * @return the configured database host
     */
    public String getDatabaseHost() {
        reloadResource();

        return getResource().getOrSetDefault("host", "localhost");
    }

    /**
     * Returns the database server port, defaulting to {@code 3306}.
     * Reloads the resource before reading.
     *
     * @return the configured database port
     */
    public int getDatabasePort() {
        reloadResource();

        return getResource().getOrSetDefault("port", 3306);
    }

    /**
     * Returns the database authentication username, defaulting to {@code "root"}.
     * Reloads the resource before reading.
     *
     * @return the configured database username
     */
    public String getDatabaseUsername() {
        reloadResource();

        return getResource().getOrSetDefault("username", "root");
    }

    /**
     * Returns the database authentication password, defaulting to {@code "password"}.
     * Reloads the resource before reading.
     *
     * @return the configured database password
     */
    public String getDatabasePassword() {
        reloadResource();

        return getResource().getOrSetDefault("password", "password");
    }

    /**
     * Returns the table name prefix used for all Streamline database tables,
     * defaulting to {@code "sl_"}. Reloads the resource before reading.
     *
     * @return the configured table prefix
     */
    public String getDatabaseTablePrefix() {
        reloadResource();

        return getResource().getOrSetDefault("table-prefix", "sl_");
    }

    /**
     * Returns the name of the database schema to connect to, defaulting to
     * {@code "streamline"}. Reloads the resource before reading.
     *
     * @return the configured database name
     */
    public String getDatabaseName() {
        reloadResource();

        return getResource().getOrSetDefault("database", "streamline");
    }

    /**
     * Returns the {@link DatabaseType} to use for the main database connection,
     * defaulting to {@link DatabaseType#SQLITE}. Reloads the resource before reading.
     *
     * @return the configured {@link DatabaseType}
     */
    public DatabaseType getDatabaseType() {
        reloadResource();

        return DatabaseType.valueOf(getResource().getOrSetDefault("type", DatabaseType.SQLITE.name()));
    }

    /**
     * Returns the file name for the SQLite database, defaulting to
     * {@code "streamline.db"}. Relevant only when the type is
     * {@link DatabaseType#SQLITE}. Reloads the resource before reading.
     *
     * @return the configured SQLite file name
     */
    public String getSqliteFileName() {
        reloadResource();

        return getResource().getOrSetDefault("sqlite-file-name", "streamline.db");
    }

    /**
     * Assembles and returns a {@link ConnectorSet} from the current configuration
     * values, ready to be passed to a database operator.
     *
     * @return a fully populated {@link ConnectorSet}
     */
    public ConnectorSet getConnectorSet() {
        return new ConnectorSet(
                getDatabaseType(),
                getDatabaseHost(),
                getDatabasePort(),
                getDatabaseName(),
                getDatabaseUsername(),
                getDatabasePassword(),
                getDatabaseTablePrefix(),
                getSqliteFileName()
        );
    }
}
