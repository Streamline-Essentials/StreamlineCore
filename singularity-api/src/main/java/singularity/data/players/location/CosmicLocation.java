package singularity.data.players.location;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.server.CosmicServer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a full cross-platform location, combining a server, world,
 * 3-D position, and rotation (yaw/pitch) into a single value object.
 *
 * <p>This class is platform-agnostic and is used throughout the Singularity
 * framework wherever a player or object position must be stored or transferred
 * across server boundaries.</p>
 */
@Getter @Setter
public class CosmicLocation implements Comparable<CosmicLocation> {

    /**
     * The server on which this location exists.
     */
    private CosmicServer server;

    /**
     * The world (dimension) in which this location exists.
     */
    private PlayerWorld world;

    /**
     * The precise X/Y/Z coordinates of this location.
     */
    private WorldPosition position;

    /**
     * The yaw and pitch rotation associated with this location.
     */
    private PlayerRotation rotation;

    /**
     * Constructs a {@code CosmicLocation} with explicit server, world, position, and rotation values.
     *
     * @param server   the target server
     * @param world    the target world
     * @param position the X/Y/Z coordinates
     * @param rotation the yaw/pitch rotation
     */
    public CosmicLocation(CosmicServer server, PlayerWorld world, WorldPosition position, PlayerRotation rotation) {
        this.server = server;
        this.world = world;
        this.position = position;
        this.rotation = rotation;
    }

    /**
     * Constructs a default (zero) {@code CosmicLocation} associated with the given player.
     * The server identifier is empty, the world is set to {@code "--null"}, and all
     * coordinates and rotation angles default to {@code 0}.
     *
     * @param player the player for whom this placeholder location is created (unused beyond context)
     */
    public CosmicLocation(CosmicPlayer player) {
        this(new CosmicServer(""), new PlayerWorld("--null"), new WorldPosition(0, 0, 0), new PlayerRotation());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Comparison order: world name first, then X/Y/Z position, then rotation
     * (yaw and pitch must both match for equality).</p>
     *
     * @param o the other location to compare against
     * @return {@code 0} if the two locations are identical; a non-zero value otherwise
     */
    @Override
    public int compareTo(@NotNull CosmicLocation o) {
        if (world.compareTo(o.world) != 0) return world.compareTo(o.world);
        if (position.compareTo(o.position) != 0) return position.compareTo(o.position);
        return rotation.getYaw() == o.rotation.getYaw() && rotation.getPitch() == o.rotation.getPitch() ? 0 : 1;
    }

    /**
     * Returns the X coordinate of this location.
     *
     * @return the X coordinate
     */
    public double getX() {
        return position.getX();
    }

    /**
     * Returns the Y coordinate of this location.
     *
     * @return the Y coordinate
     */
    public double getY() {
        return position.getY();
    }

    /**
     * Returns the Z coordinate of this location.
     *
     * @return the Z coordinate
     */
    public double getZ() {
        return position.getZ();
    }

    /**
     * Returns the yaw angle (horizontal rotation) of this location.
     *
     * @return the yaw in degrees
     */
    public float getYaw() {
        return rotation.getYaw();
    }

    /**
     * Returns the pitch angle (vertical rotation) of this location.
     *
     * @return the pitch in degrees
     */
    public float getPitch() {
        return rotation.getPitch();
    }

    /**
     * Converts the floating-point position of this location to integer block coordinates.
     *
     * @return a {@link BlockPosition} representing the rounded block coordinates
     */
    public BlockPosition asBlockPosition() {
        return getPosition().asBlockPosition();
    }

    /**
     * Returns the block-level X coordinate (rounded from the precise X value).
     *
     * @return the block X coordinate
     */
    public int getBlockX() {
        return asBlockPosition().getX();
    }

    /**
     * Returns the block-level Y coordinate (rounded from the precise Y value).
     *
     * @return the block Y coordinate
     */
    public int getBlockY() {
        return asBlockPosition().getY();
    }

    /**
     * Returns the block-level Z coordinate (rounded from the precise Z value).
     *
     * @return the block Z coordinate
     */
    public int getBlockZ() {
        return asBlockPosition().getZ();
    }

    /**
     * Returns the name (identifier) of the world in which this location exists.
     *
     * @return the world name
     */
    public String getWorldName() {
        return world.getIdentifier();
    }

    /**
     * Returns the name (identifier) of the server on which this location exists.
     *
     * @return the server name
     */
    public String getServerName() {
        return getServer().getIdentifier();
    }

    /**
     * Sets the X coordinate of this location and returns {@code this} for chaining.
     *
     * @param x the new X coordinate
     * @return this location instance
     */
    public CosmicLocation setX(double x) {
        position.setX(x);
        return this;
    }

    /**
     * Sets the Y coordinate of this location and returns {@code this} for chaining.
     *
     * @param y the new Y coordinate
     * @return this location instance
     */
    public CosmicLocation setY(double y) {
        position.setY(y);
        return this;
    }

    /**
     * Sets the Z coordinate of this location and returns {@code this} for chaining.
     *
     * @param z the new Z coordinate
     * @return this location instance
     */
    public CosmicLocation setZ(double z) {
        position.setZ(z);
        return this;
    }

    /**
     * Sets the yaw angle of this location and returns {@code this} for chaining.
     *
     * @param yaw the new yaw in degrees
     * @return this location instance
     */
    public CosmicLocation setYaw(float yaw) {
        rotation.setYaw(yaw);
        return this;
    }

    /**
     * Sets the pitch angle of this location and returns {@code this} for chaining.
     *
     * @param pitch the new pitch in degrees
     * @return this location instance
     */
    public CosmicLocation setPitch(float pitch) {
        rotation.setPitch(pitch);
        return this;
    }

    /**
     * Replaces the world of this location with a new {@link PlayerWorld} for the given name,
     * and returns {@code this} for chaining.
     *
     * @param worldName the new world name
     * @return this location instance
     */
    public CosmicLocation setWorldName(String worldName) {
        world = new PlayerWorld(worldName);
        return this;
    }

    /**
     * Replaces the server of this location with a new {@link CosmicServer} for the given name,
     * and returns {@code this} for chaining.
     *
     * @param serverName the new server name
     * @return this location instance
     */
    public CosmicLocation setServerName(String serverName) {
        server = new CosmicServer(serverName);
        return this;
    }

    /**
     * Serializes this location to a human-readable string in the format:
     * {@code [server=<name>,world=<name>,position={x=<x>,y=<y>,z=<z>},rotation={yaw=<yaw>,pitch=<pitch>}]}.
     *
     * @return a string representation of this location
     */
    public String asString() {
        return "[" +
                "server=" + getServerName() +
                ",world=" + getWorldName() +
                ",position={"
                + "x=" + getX() +
                ",y=" + getY() +
                ",z=" + getZ() +
                "}" +
                ",rotation={"
                + "yaw=" + getYaw() +
                ",pitch=" + getPitch() +
                "}" +
                "]";
    }

    /**
     * Teleports the given player to this location via the platform's user manager.
     *
     * @param otherPlayer the player to teleport
     */
    public void teleport(CosmicPlayer otherPlayer) {
        Singularity.getInstance().getUserManager().teleport(otherPlayer, this);
    }

    /**
     * Calculates the Euclidean distance between this location and another.
     * This calculation is purely coordinate-based and ignores server/world boundaries.
     *
     * @param other the other location
     * @return the straight-line distance between the two positions
     */
    public double distance(CosmicLocation other) {
        return Math.sqrt(Math.pow(getX() - other.getX(), 2) +
                Math.pow(getY() - other.getY(), 2) +
                Math.pow(getZ() - other.getZ(), 2));
    }
}
