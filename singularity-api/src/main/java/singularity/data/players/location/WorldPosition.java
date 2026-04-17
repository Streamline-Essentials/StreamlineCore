package singularity.data.players.location;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a precise 3-D position within a Minecraft world using double-precision
 * floating-point coordinates.
 *
 * <p>Comparison is performed component-wise: X first, then Y, then Z, using
 * {@link Double#compare(double, double)} semantics.</p>
 */
@Getter @Setter
public class WorldPosition implements Comparable<WorldPosition> {

    /**
     * The X coordinate (east/west axis).
     */
    private double x;

    /**
     * The Y coordinate (vertical axis).
     */
    private double y;

    /**
     * The Z coordinate (north/south axis).
     */
    private double z;

    /**
     * Constructs a {@code WorldPosition} with the given coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public WorldPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Positions are ordered by X, then Y, then Z.</p>
     *
     * @param o the other position to compare against
     * @return a negative integer, zero, or a positive integer as this position
     *         is less than, equal to, or greater than {@code o}
     */
    @Override
    public int compareTo(@NotNull WorldPosition o) {
        if (x != o.x) return Double.compare(x, o.x);
        if (y != o.y) return Double.compare(y, o.y);
        return Double.compare(z, o.z);
    }

    /**
     * Returns a new {@code WorldPosition} with the same X, Y, and Z values as this one.
     *
     * @return a copy of this position
     */
    public WorldPosition copy() {
        return new WorldPosition(x, y, z);
    }

    /**
     * Calculates the Euclidean distance between this position and another.
     *
     * @param other the other position
     * @return the straight-line distance
     */
    public double distance(WorldPosition other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    /**
     * Converts this floating-point position to integer block coordinates by rounding
     * each axis to the nearest integer.
     *
     * @return a {@link BlockPosition} representing the rounded block coordinates
     */
    public BlockPosition asBlockPosition() {
        int x = (int) Math.round(this.x);
        int y = (int) Math.round(this.y);
        int z = (int) Math.round(this.z);

        return new BlockPosition(x, y, z);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the position as {@code "<x>;<y>;<z>"}.</p>
     */
    @Override
    public String toString() {
        return getX() + ";" + getY() + ";" + getZ();
    }
}
