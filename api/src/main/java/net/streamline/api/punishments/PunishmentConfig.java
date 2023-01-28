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

        String use = getResource().getOrSetDefault("saving.use", StorageUtils.SupportedStorageType.YAML.toString());
        if (use == null) {
            use = StorageUtils.SupportedStorageType.YAML.toString();
        } else {
            if (use.equals("")) {
                use = StorageUtils.SupportedStorageType.YAML.toString();
            }
        }

        return StorageUtils.SupportedStorageType.valueOf(use);
    }

    public String getDatabaseConnectionUri() {
        reloadResource();

        return getResource().getOrSetDefault("saving.databases.connection-uri", "mongodb://<user>:<pass>@<host>:<port>/?authSource=admin&readPreference=primary&appname=StreamlineAPI&ssl=false");
    }

    public String getDatabasePrefix() {
        reloadResource();

        return getResource().getOrSetDefault("saving.databases.prefix", "sl_");
    }

    public DatabaseConfig getConfiguredDatabase() {
        String typeString = getResource().getString("saving.databases.type");
        if (typeString == null) {
            return null;
        }
        try {
            StorageUtils.SupportedDatabaseType type = StorageUtils.SupportedDatabaseType.valueOf(typeString);
            return new DatabaseConfig(type, getDatabaseConnectionUri(), getDatabasePrefix());
        } catch (Exception e) {
//            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void init() {
        isEnabled();
        getUseType();
        getDatabaseConnectionUri();
        getDatabasePrefix();
    }
}
