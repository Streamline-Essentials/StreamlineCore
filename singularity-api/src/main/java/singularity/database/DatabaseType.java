package singularity.database;

import lombok.Getter;

/**
 * Enumerates the database engines supported by the StreamlineCore storage layer.
 *
 * <p>Each constant carries the JDBC URL prefix and the fully-qualified JDBC driver
 * class name required to establish a connection via HikariCP.</p>
 */
@Getter
public enum DatabaseType {

    /**
     * MySQL (or compatible, e.g. MariaDB) relational database.
     * Uses the {@code com.mysql.cj.jdbc.Driver} JDBC driver.
     */
    MYSQL("jdbc:mysql://", "com.mysql.cj.jdbc.Driver"),

    /**
     * Embedded SQLite file-based database.
     * Uses the {@code org.sqlite.JDBC} driver.
     */
    SQLITE("jdbc:sqlite:", "org.sqlite.JDBC"),
    ;

    /**
     * The JDBC URL prefix for this database type (e.g. {@code jdbc:mysql://}).
     * Combined with host, port, and database name to form the full connection URL.
     */
    private final String urlPrefix;

    /**
     * The fully-qualified class name of the JDBC driver for this database type.
     */
    private final String driver;

    /**
     * Constructs a {@code DatabaseType} constant with its associated JDBC metadata.
     *
     * @param urlPrefix the JDBC URL prefix string
     * @param driver    the fully-qualified JDBC driver class name
     */
    DatabaseType(String urlPrefix, String driver) {
        this.urlPrefix = urlPrefix;
        this.driver = driver;
    }
}
