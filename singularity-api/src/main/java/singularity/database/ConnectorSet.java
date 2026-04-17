package singularity.database;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds all configuration values needed to establish a JDBC database connection.
 *
 * <p>A {@code ConnectorSet} is consumed by {@link DBOperator} when building a
 * HikariCP connection pool. Depending on the {@link DatabaseType}, different fields
 * are relevant:</p>
 * <ul>
 *   <li><strong>MYSQL</strong> — {@code host}, {@code port}, {@code database},
 *       {@code username}, {@code password}, and {@code tablePrefix} are used.</li>
 *   <li><strong>SQLITE</strong> — only {@code sqliteFileName} and
 *       {@code tablePrefix} are used; the connection URL is built from the
 *       database storage folder and the file name.</li>
 * </ul>
 */
@Getter @Setter
public class ConnectorSet {

    /** The database engine to connect to (MySQL or SQLite). */
    private DatabaseType type;

    /** The hostname or IP address of the MySQL server. Not used for SQLite. */
    private String host;

    /** The TCP port on which the MySQL server listens. Not used for SQLite. */
    private int port;

    /** The name of the MySQL database/schema to select. Not used for SQLite. */
    private String database;

    /** The username for authenticating with the MySQL server. Not used for SQLite. */
    private String username;

    /** The password for authenticating with the MySQL server. Not used for SQLite. */
    private String password;

    /**
     * An optional prefix prepended to every table name created by this operator,
     * allowing multiple logical databases to share a single schema.
     */
    private String tablePrefix;

    /**
     * The file name (relative to the plugin's storage folder) of the SQLite database
     * file. Only used when {@link #type} is {@link DatabaseType#SQLITE}.
     */
    private String sqliteFileName;

    /**
     * Constructs a fully specified {@code ConnectorSet}.
     *
     * @param type           the database engine type
     * @param host           MySQL server hostname (ignored for SQLite)
     * @param port           MySQL server port (ignored for SQLite)
     * @param database       MySQL database name (ignored for SQLite)
     * @param username       MySQL username (ignored for SQLite)
     * @param password       MySQL password (ignored for SQLite)
     * @param tablePrefix    prefix to prepend to all table names
     * @param sqliteFileName SQLite database file name (ignored for MySQL)
     */
    public ConnectorSet(DatabaseType type, String host, int port, String database, String username, String password, String tablePrefix, String sqliteFileName) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablePrefix = tablePrefix;
        this.sqliteFileName = sqliteFileName;
    }

    /**
     * Builds the JDBC connection URI for the configured {@link DatabaseType}.
     *
     * <ul>
     *   <li>For {@link DatabaseType#MYSQL}: {@code jdbc:mysql://host:port/database}</li>
     *   <li>For {@link DatabaseType#SQLITE}: the URL prefix only (the file path is
     *       appended later by {@link DBOperator#buildDataSource()}).</li>
     *   <li>For any other type: an empty string.</li>
     * </ul>
     *
     * @return the JDBC URI string appropriate for the configured database type
     */
    public String getUri() {
        switch (type) {
            case MYSQL:
                return type.getUrlPrefix() + host + ":" + port + "/" + database;
            case SQLITE:
                return type.getUrlPrefix();
            default:
                return "";
        }
    }
}
