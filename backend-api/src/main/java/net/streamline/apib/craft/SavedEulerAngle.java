package net.streamline.apib.craft;

import de.leonhard.storage.sections.FlatFileSection;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.EulerAngle;

public class SavedEulerAngle {
    @Getter @Setter
    public double x, y, z;

    public SavedEulerAngle(EulerAngle from) {
        this.x = from.getX();
        this.y = from.getY();
        this.z = from.getZ();
    }

    public SavedEulerAngle(FlatFileSection section) {
        setX(section.getDouble("x"));
        setY(section.getDouble("y"));
        setZ(section.getDouble("z"));
    }

    public void saveInto(FlatFileSection section) {
        section.set("x", getX());
        section.set("y", getY());
        section.set("z", getZ());
    }

    public EulerAngle get() {
        return new EulerAngle(x, y, z);
    }
}
