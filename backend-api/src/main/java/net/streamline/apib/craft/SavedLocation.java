package net.streamline.apib.craft;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

/**
 * A serialisable snapshot of a Bukkit {@link Location} that stores the world
 * name, coordinates, yaw, and pitch as primitive fields. Instances can be
 * reconstructed into a live {@link Location} at any time via {@link #get()}.
 */
@Getter
public class SavedLocation {

    /** The name of the world this location belongs to. */
    @Setter
    private String world;

    /** The X, Y, and Z block coordinates of this location. */
    @Setter
    private double x, y, z;

    /** The horizontal (yaw) and vertical (pitch) facing angles in degrees. */
    @Setter
    private float yaw, pitch;

    /**
     * Creates a {@code SavedLocation} by copying all components from an
     * existing Bukkit {@link Location}.
     *
     * @param from the source location to copy; its world must not be {@code null}
     * @throws NullPointerException if the world of {@code from} is {@code null}
     */
    public SavedLocation(Location from) {
        setWorld(Objects.requireNonNull(from.getWorld()).getName());
        setX(from.getX());
        setY(from.getY());
        setZ(from.getZ());
        setYaw(from.getYaw());
        setPitch(from.getPitch());
    }

    /**
     * Converts this saved snapshot back to a live Bukkit {@link Location} by
     * looking up the world by name from the current server.
     *
     * @return a new {@link Location} using the stored world, coordinates, yaw,
     *         and pitch; the world field may be {@code null} if the world is
     *         not currently loaded
     */
    public Location get() {
        return new Location(Bukkit.getServer().getWorld(getWorld()),
                getX(), getY(), getZ(),
                getYaw(), getPitch()
        );
    }
}
