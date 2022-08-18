package net.streamline.apib.craft;

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

    public EulerAngle get() {
        return new EulerAngle(x, y, z);
    }
}
