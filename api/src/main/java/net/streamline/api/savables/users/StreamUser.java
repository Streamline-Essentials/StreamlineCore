package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.datalizable.properties.Property;
import net.streamline.api.savables.datalizable.properties.Container;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter @Setter
public class StreamUser implements Comparable<StreamUser> {
    private final UUID _id; // internal ID.

    private final Container container; // container for this user.

    public StreamUser(UUID _id, UUID uuid, String username, String displayName) {
        this._id = _id;

        this.container = new Container(_id); // create a new container for this user.
    }

    public StreamUser(UUID uuid, String username) {
        this(UUID.randomUUID(), uuid, username, username);
    }

    public StreamUser(UUID uuid) {
        this(uuid, uuid.toString());
    }

    public void setUsername(String username) {
        this.container.put(Property.USERNAME, username);
    }

    public String getUsername() {
        return this.container.get(Property.USERNAME);
    }

    public void setDisplayName(String displayName) {
        this.container.put(Property.DISPLAY_NAME, displayName);
    }

    public String getDisplayName() {
        return this.container.get(Property.DISPLAY_NAME);
    }

    public void setServer(String server) {
        this.container.put(Property.SERVER, server);
    }

    public String getServer() {
        return this.container.get(Property.SERVER);
    }

    public void setWorld(String world) {
        this.container.put(Property.WORLD, world);
    }

    public String getWorld() {
        return this.container.get(Property.WORLD);
    }

    public void setPosition(String position) {
        this.container.put(Property.POSITION, position);
    }

    public String getPosition() {
        return this.container.get(Property.POSITION);
    }

    public void setRotation(String rotation) {
        this.container.put(Property.ROTATION, rotation);
    }

    public String getRotation() {
        return this.container.get(Property.ROTATION);
    }

    public void setFirstJoin(long firstJoin) {
        this.container.put(Property.FIRST_JOIN, firstJoin);
    }

    public long getFirstJoin() {
        return this.container.get(Property.FIRST_JOIN);
    }

    public void setLastJoin(long lastJoin) {
        this.container.put(Property.LAST_JOIN, lastJoin);
    }

    public long getLastJoin() {
        return this.container.get(Property.LAST_JOIN);
    }

    public void setLastQuit(long lastQuit) {
        this.container.put(Property.LAST_QUIT, lastQuit);
    }

    public long getLastQuit() {
        return this.container.get(Property.LAST_QUIT);
    }

    @Override
    public int compareTo(@NotNull StreamUser o) {
        return this._id.compareTo(o._id);
    }

    public void push() {

    }
}
