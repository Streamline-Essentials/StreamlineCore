package net.streamline.apib.craft;

import gg.drak.thebase.lib.leonhard.storage.sections.FlatFileSection;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.EulerAngle;

/**
 * A serialisable representation of a Bukkit {@link EulerAngle} that can be
 * persisted to and restored from a flat-file configuration section. Stores
 * the X, Y, and Z rotation components as plain doubles.
 */
@Getter
public class SavedEulerAngle {

    /**
     * The X, Y, and Z rotation angles in radians.
     */
    @Setter
    public double x, y, z;

    /**
     * Creates a {@code SavedEulerAngle} by copying the rotation components from
     * an existing Bukkit {@link EulerAngle}.
     *
     * @param from the source {@link EulerAngle} to copy
     */
    public SavedEulerAngle(EulerAngle from) {
        this.x = from.getX();
        this.y = from.getY();
        this.z = from.getZ();
    }

    /**
     * Restores a {@code SavedEulerAngle} from a flat-file configuration section
     * by reading the {@code x}, {@code y}, and {@code z} keys as doubles.
     *
     * @param section the configuration section containing the angle data
     */
    public SavedEulerAngle(FlatFileSection section) {
        setX(section.getDouble("x"));
        setY(section.getDouble("y"));
        setZ(section.getDouble("z"));
    }

    /**
     * Persists the current angle components into the given flat-file section
     * under the keys {@code x}, {@code y}, and {@code z}.
     *
     * @param section the configuration section to write the angle data into
     */
    public void saveInto(FlatFileSection section) {
        section.set("x", getX());
        section.set("y", getY());
        section.set("z", getZ());
    }

    /**
     * Converts this saved representation back to a Bukkit {@link EulerAngle}.
     *
     * @return a new {@link EulerAngle} constructed from the stored X, Y, and Z values
     */
    public EulerAngle get() {
        return new EulerAngle(x, y, z);
    }
}
