package net.streamline.api.savables.database;

import lombok.Getter;

@Getter
public enum ConnectionType {
    MYSQL(
        "jdbc:mysql://",
        "com.mysql.jdbc.Driver"
    ),
    SQLITE(
        "jdbc:sqlite:",
        "org.sqlite.JDBC"
    ),
    ;

    private final String urlPrefix;
    private final String driver;

    ConnectionType(String urlPrefix, String driver) {
        this.urlPrefix = urlPrefix;
        this.driver = driver;
    }

    public static ConnectionType fromString(String type) {
        for (ConnectionType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return SQLITE;
    }
}
