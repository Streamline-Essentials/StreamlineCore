package net.streamline.apib.craft;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

@Getter
public class SavedLocation {
    @Setter
    private String world;
    @Setter
    private double x, y, z;
    @Setter
    private float yaw, pitch;

    public SavedLocation(Location from) {
        setWorld(Objects.requireNonNull(from.getWorld()).getName());
        setX(from.getX());
        setY(from.getY());
        setZ(from.getZ());
        setYaw(from.getYaw());
        setPitch(from.getPitch());
    }

    public Location get() {
        return new Location(Bukkit.getServer().getWorld(getWorld()),
                getX(), getY(), getZ(),
                getYaw(), getPitch()
        );
    }
}
