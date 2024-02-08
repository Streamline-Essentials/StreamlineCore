package net.streamline.api.savables.datalizable.properties;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamUser;
import tv.quaint.thebase.lib.bson.Document;

import java.util.UUID;

@Getter @Setter
public class Container extends Document {
    private final UUID belongsTo; // Internal ID of the user this container belongs to.

    public Container(UUID belongsTo) {
        this.belongsTo = belongsTo;
    }

    public void put(Property property, Object value) {
        put(property.get(), value);
    }

    public <T> T get(Property property) {
        return (T) get(property.get());
    }
}
