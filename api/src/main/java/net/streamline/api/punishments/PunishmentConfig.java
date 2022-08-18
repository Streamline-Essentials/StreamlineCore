package net.streamline.api.punishments;

import de.leonhard.storage.Config;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.DatabaseConfig;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.configs.StorageUtils;

public class PunishmentConfig extends FlatFileResource<Config> {
    public PunishmentConfig() {
        super(Config.class, "punishment-config.yml", SLAPI.getInstance().getDataFolder(), true);
    }

    public boolean isEnabled() {
        reloadResource();

        return resource.getOrSetDefault("enabled", false);
    }

    public StorageUtils.StorageType getUseType() {
        reloadResource();

        return StorageUtils.StorageType.valueOf(resource.getOrSetDefault("saving.use", StorageUtils.StorageType.YAML.toString()));
    }

    public String getDatabaseConnectionUri() {
        reloadResource();

        return resource.getOrSetDefault("saving.databases.connection-uri", "mongodb://<user>:<pass>@<host>:<port>/?authSource=admin&readPreference=primary&appname=StreamlineAPI&ssl=false");
    }

    public String getDatabaseDatabase() {
        reloadResource();

        return resource.getOrSetDefault("saving.databases.database", "streamline_users");
    }

    public String getDatabasePrefix() {
        reloadResource();

        return resource.getOrSetDefault("saving.databases.prefix", "sl_");
    }

    public DatabaseConfig getConfiguredDatabase() {
        StorageUtils.DatabaseType databaseType = null;
        if (getUseType().equals(StorageUtils.StorageType.MONGO)) databaseType = StorageUtils.DatabaseType.MONGO;
        if (getUseType().equals(StorageUtils.StorageType.MYSQL)) databaseType = StorageUtils.DatabaseType.MYSQL;
        if (databaseType == null) return null;

        return new DatabaseConfig(getDatabaseConnectionUri(), getDatabaseDatabase(), getDatabasePrefix(), databaseType);
    }
}
