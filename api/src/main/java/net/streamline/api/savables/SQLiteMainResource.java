package net.streamline.api.savables;

import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.databases.specific.SQLiteResource;

public class SQLiteMainResource extends SQLiteResource {
    public SQLiteMainResource(DatabaseConfig config) {
        super(config);
    }
}
