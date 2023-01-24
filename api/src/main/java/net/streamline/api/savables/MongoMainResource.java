package net.streamline.api.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlineUser;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.databases.specific.MongoResource;

public class MongoMainResource extends MongoResource {
    public MongoMainResource(DatabaseConfig config) {
        super(config);
    }
}
