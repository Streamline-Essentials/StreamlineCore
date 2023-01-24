package net.streamline.api.savables;

import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.databases.specific.MySQLResource;

public class MySQLMainResource extends MySQLResource {
    public MySQLMainResource(DatabaseConfig config) {
        super(config);
    }
}
