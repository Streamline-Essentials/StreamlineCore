package net.streamline.api.savables.datalizable;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.datalizable.properties.Container;
import tv.quaint.objects.Identifiable;

import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

@Getter @Setter
public class DataDocument implements Identifiable {
    private final UUID _id; // internal ID.
    private String identifier; // table label. ex: "users", "locations", "homes", etc.
    private Container dataMap; // the data map for this table

    @Getter @Setter
    private static ConcurrentSkipListMap<String, String> foreignValues = new ConcurrentSkipListMap<>(); // the foreign table and where to find the value for it.

    public static void addForeignValue(String foreignTable, String value) {
        foreignValues.put(foreignTable, value);
    }

    public static void removeForeignValue(String foreignTable) {
        foreignValues.remove(foreignTable);
    }

    public DataDocument(String identifier) {
        this._id = UUID.randomUUID();

        this.identifier = identifier;

        this.dataMap = new Container(_id);
    }

    public void push() {

    }
}
