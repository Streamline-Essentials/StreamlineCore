package net.streamline.api.punishments;

import net.streamline.api.SLAPI;
import tv.quaint.storage.StorageUtils;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;
import tv.quaint.thebase.lib.leonhard.storage.sections.FlatFileSection;

public class PunishmentConfig extends SimpleConfiguration {
    public PunishmentConfig() {
        super("punishment-config.yml", SLAPI.getInstance().getDataFolder(), true);
    }

    public boolean isEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("enabled", false);
    }

    public StorageUtils.SupportedStorageType getUseType() {
        reloadResource();

        return StorageUtils.SupportedStorageType.valueOf(getResource().getOrSetDefault("saving.use", StorageUtils.SupportedStorageType.YAML.toString()));
    }

    public String getDatabaseConnectionUri() {
        reloadResource();

        return getResource().getOrSetDefault("saving.databases.connection-uri", "mongodb://<user>:<pass>@<host>:<port>/?authSource=admin&readPreference=primary&appname=StreamlineAPI&ssl=false");
    }

    public String getDatabaseDatabase() {
        reloadResource();

        return getResource().getOrSetDefault("saving.databases.database", "streamline_users");
    }

    public String getDatabasePrefix() {
        reloadResource();

        return getResource().getOrSetDefault("saving.databases.prefix", "sl_");
    }

    public DatabaseConfig getConfiguredDatabase() {
        FlatFileSection section = getResource().getSection("database");

        StorageUtils.SupportedDatabaseType type = StorageUtils.SupportedDatabaseType.valueOf(section.getOrSetDefault("type", StorageUtils.SupportedDatabaseType.SQLITE.toString()));
        String link;
        switch (type) {
            case MONGO:
                link = section.getOrDefault("link", "mongodb://{{user}}:{{pass}}@{{host}}:{{port}}/{{database}}");
                break;
            case MYSQL:
                link = section.getOrDefault("link", "jdbc:mysql://{{host}}:{{port}}/{{database}}{{options}}");
                break;
            case SQLITE:
                link = section.getOrDefault("link", "jdbc:sqlite:{{database}}.db");
                break;
            default:
                link = section.getOrSetDefault("link", "jdbc:sqlite:{{database}}.db");
                break;
        }
        String host = section.getOrSetDefault("host", "localhost");
        int port = section.getOrSetDefault("port", 3306);
        String username = section.getOrSetDefault("username", "user");
        String password = section.getOrSetDefault("password", "pass1234");
        String database = section.getOrSetDefault("database", "streamline");
        String tablePrefix = section.getOrSetDefault("table-prefix", "sl_");
        String options = section.getOrSetDefault("options", "?useSSL=false&serverTimezone=UTC");

        return new DatabaseConfig(type, link, host, port, username, password, database, tablePrefix, options);
    }

    @Override
    public void init() {
        isEnabled();
        getUseType();
        getDatabaseConnectionUri();
        getDatabaseDatabase();
        getDatabasePrefix();
    }
}
