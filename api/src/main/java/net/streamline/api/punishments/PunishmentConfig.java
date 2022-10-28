package net.streamline.api.punishments;

import net.streamline.api.SLAPI;
import tv.quaint.storage.StorageUtils;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

public class PunishmentConfig extends SimpleConfiguration {
    public PunishmentConfig() {
        super("punishment-config.yml", SLAPI.getDataFolder(), true);
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
        StorageUtils.SupportedDatabaseType databaseType = null;
        if (getUseType().equals(StorageUtils.SupportedStorageType.MONGO)) databaseType = StorageUtils.SupportedDatabaseType.MONGO;
        if (getUseType().equals(StorageUtils.SupportedStorageType.MYSQL)) databaseType = StorageUtils.SupportedDatabaseType.MYSQL;
        if (databaseType == null) return null;

        return new DatabaseConfig(getDatabaseConnectionUri(), getDatabaseDatabase(), getDatabasePrefix(), databaseType);
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
