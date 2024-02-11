package net.streamline.api.savables.database;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConnectionSet {
    private ConnectionType type;
    private String host;
    private String database;
    private String username;
    private String password;
    private int port;
    private String sqliteFile;

    public ConnectionSet(ConnectionType type, String host, String database, String username, String password, int port, String sqliteFile) {
        this.type = type;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
        this.sqliteFile = sqliteFile;
    }

    public String getDriver() {
        return getType().getDriver();
    }

    public String getUrl() {
        switch (getType()) {
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s", getHost(), getPort(), getDatabase());
            case SQLITE:
                return String.format("jdbc:sqlite:%s", getSqliteFile());
            default:
                return null;
        }
    }
}
