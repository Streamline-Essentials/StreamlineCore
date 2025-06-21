package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.ConnectorSet;
import singularity.database.DatabaseType;

@Getter @Setter
public class DatabaseConfigHandler extends SimpleConfiguration {
    public DatabaseConfigHandler() {
        super("database-config.yml", Singularity.getInstance(), true);
    }

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

    // DATABASE

    public String getDatabaseHost() {
        reloadResource();

        return getResource().getOrSetDefault("host", "localhost");
    }

    public int getDatabasePort() {
        reloadResource();

        return getResource().getOrSetDefault("port", 3306);
    }

    public String getDatabaseUsername() {
        reloadResource();

        return getResource().getOrSetDefault("username", "root");
    }

    public String getDatabasePassword() {
        reloadResource();

        return getResource().getOrSetDefault("password", "password");
    }

    public String getDatabaseTablePrefix() {
        reloadResource();

        return getResource().getOrSetDefault("table-prefix", "sl_");
    }

    public String getDatabaseName() {
        reloadResource();

        return getResource().getOrSetDefault("database", "streamline");
    }

    public DatabaseType getDatabaseType() {
        reloadResource();

        return DatabaseType.valueOf(getResource().getOrSetDefault("type", DatabaseType.SQLITE.name()));
    }

    public String getSqliteFileName() {
        reloadResource();

        return getResource().getOrSetDefault("sqlite-file-name", "streamline.db");
    }

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
