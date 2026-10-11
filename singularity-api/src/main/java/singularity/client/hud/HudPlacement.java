package singularity.client.hud;

import lombok.Getter;
import lombok.Setter;

/**
 * Where the player put one {@link HudElement}: its top-left corner as a fraction of the
 * GUI-scaled screen (so it keeps its place across window sizes), its scale, and whether it
 * is shown.
 */
@Getter
@Setter
public class HudPlacement {

    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 4.0;

    private volatile double x;
    private volatile double y;
    private volatile double scale;
    private volatile boolean enabled;

    public HudPlacement(double x, double y, double scale, boolean enabled) {
        this.x = clamp(x, 0, 1);
        this.y = clamp(y, 0, 1);
        this.scale = clamp(scale, MIN_SCALE, MAX_SCALE);
        this.enabled = enabled;
    }

    public void moveTo(double x, double y) {
        this.x = clamp(x, 0, 1);
        this.y = clamp(y, 0, 1);
    }

    public void scaleTo(double scale) {
        this.scale = clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
