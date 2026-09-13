package singularity.objects.world;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;

/**
 * A platform-agnostic representation of a block in a Minecraft world.
 *
 * <p>A {@code CosmicBlock} combines a {@link PlayerWorld}, a three-dimensional
 * {@link WorldPosition}, and a material type string. It supports fluent
 * position mutation and natural ordering via {@link Comparable}.
 */
@Getter @Setter
public class CosmicBlock implements Comparable<CosmicBlock> {

    /**
     * The world this block belongs to.
     * Lombok generates {@code getWorld()} and {@code setWorld(PlayerWorld)};
     * an additional fluent overload accepting a name string is provided by
     * {@link #setWorld(String)}.
     */
    private PlayerWorld world;

    /**
     * The three-dimensional position of this block within its world.
     */
    private WorldPosition location;

    /**
     * The material type of this block, expressed as a namespaced-key or
     * legacy name string (e.g. {@code "STONE"} or {@code "minecraft:stone"}).
     */
    private String type;

    /**
     * Constructs a new {@code CosmicBlock} with the supplied world, position, and type.
     *
     * @param world    the world the block resides in
     * @param location the position of the block
     * @param type     the material type of the block
     */
    public CosmicBlock(PlayerWorld world, WorldPosition location, String type) {
        this.world = world;
        this.location = location;
        this.type = type;
    }

    /**
     * Returns the X coordinate of this block's position.
     *
     * @return the X coordinate
     */
    public double getX() {
        return location.getX();
    }

    /**
     * Returns the Y coordinate of this block's position.
     *
     * @return the Y coordinate
     */
    public double getY() {
        return location.getY();
    }

    /**
     * Returns the Z coordinate of this block's position.
     *
     * @return the Z coordinate
     */
    public double getZ() {
        return location.getZ();
    }

    /**
     * Returns the identifier (name) of the world this block is in.
     *
     * @return the world name as reported by {@link PlayerWorld#getIdentifier()}
     */
    public String getWorldName() {
        return world.getIdentifier();
    }

    /**
     * Sets the X coordinate of this block's position and returns {@code this}
     * for fluent chaining.
     *
     * @param x the new X coordinate
     * @return this {@code CosmicBlock} instance
     */
    public CosmicBlock setX(double x) {
        this.location.setX(x);
        return this;
    }

    /**
     * Sets the Y coordinate of this block's position and returns {@code this}
     * for fluent chaining.
     *
     * @param y the new Y coordinate
     * @return this {@code CosmicBlock} instance
     */
    public CosmicBlock setY(double y) {
        this.location.setY(y);
        return this;
    }

    /**
     * Sets the Z coordinate of this block's position and returns {@code this}
     * for fluent chaining.
     *
     * @param z the new Z coordinate
     * @return this {@code CosmicBlock} instance
     */
    public CosmicBlock setZ(double z) {
        this.location.setZ(z);
        return this;
    }

    /**
     * Sets the world of this block using a {@link PlayerWorld} instance and
     * returns {@code this} for fluent chaining.
     *
     * <p>Note: this overload shadows the Lombok-generated setter; unlike the
     * Lombok setter it returns {@code this} rather than {@code void}.
     *
     * @param world the new world
     * @return this {@code CosmicBlock} instance
     */
    public CosmicBlock setWorld(PlayerWorld world) {
        this.world = world;
        return this;
    }

    /**
     * Sets the world of this block by name, constructing a new {@link PlayerWorld},
     * and returns {@code this} for fluent chaining.
     *
     * @param worldName the name of the world
     * @return this {@code CosmicBlock} instance
     */
    public CosmicBlock setWorld(String worldName) {
        this.world = new PlayerWorld(worldName);
        return this;
    }

    /**
     * Computes the Euclidean distance between this block and another.
     *
     * @param other the other block; if {@code null}, {@code -1} is returned
     * @return the distance, or {@code -1} if {@code other} is {@code null}
     */
    public double distance(CosmicBlock other) {
        if (other == null) return -1d;

        return getLocation().distance(other.getLocation());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks are first ordered by their world (via {@link PlayerWorld#compareTo}),
     * and within the same world by their {@link WorldPosition}.
     *
     * @param o the other block to compare against; must not be {@code null}
     * @return a negative integer, zero, or a positive integer as this block is
     *         less than, equal to, or greater than {@code o}
     */
    @Override
    public int compareTo(@NotNull CosmicBlock o) {
        if (this.world != o.world) {
            return this.world.compareTo(o.world);
        } else {
            return this.location.compareTo(o.location);
        }
    }
}
