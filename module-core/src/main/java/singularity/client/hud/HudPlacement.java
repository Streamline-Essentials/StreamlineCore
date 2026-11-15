package singularity.client.hud;

import lombok.Getter;
import lombok.Setter;

/**
 * Where the player put one {@link HudElement}: its top-left corner as a fraction of the
 * GUI-scaled screen (so it keeps its place across window sizes), its scale, and whether it
 * is shown, plus the frame drawn around it and the {@link HudGroup} it belongs to, if any.
 *
 * <p>A hidden element stays in the HUD editor, dimmed, to be shown again there; a removed one
 * is also hidden but leaves the editor too, and comes back only from the module menu's toggle.</p>
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
    private volatile boolean removed;
    /** The frame drawn around the element; never {@code null}. */
    private final HudBoxStyle style;
    /** The id of the {@link HudGroup} the element moves with, or {@code null}. */
    private volatile String group;

    public HudPlacement(double x, double y, double scale, boolean enabled) {
        this(x, y, scale, enabled, false);
    }

    public HudPlacement(double x, double y, double scale, boolean enabled, boolean removed) {
        this(x, y, scale, enabled, removed, new HudBoxStyle(), null);
    }

    public HudPlacement(double x, double y, double scale, boolean enabled, boolean removed, HudBoxStyle style, String group) {
        this.style = style == null ? new HudBoxStyle() : style;
        this.group = group == null || group.isEmpty() ? null : group;
        this.x = clamp(x, 0, 1);
        this.y = clamp(y, 0, 1);
        this.scale = clamp(scale, MIN_SCALE, MAX_SCALE);
        this.enabled = enabled && ! removed;
        this.removed = removed;
    }

    public HudPlacement copy() {
        return new HudPlacement(x, y, scale, enabled, removed, style.copy(), group);
    }

    /** Hides the element and takes it out of the HUD editor. */
    public void remove() {
        enabled = false;
        removed = true;
    }

    /** Shows the element, bringing it back to the HUD editor if it was removed. */
    public void show() {
        enabled = true;
        removed = false;
    }

    public void moveTo(double x, double y) {
        this.x = clamp(x, 0, 1);
        this.y = clamp(y, 0, 1);
    }

    public void scaleTo(double scale) {
        this.scale = clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    /**
     * @return whether {@code other} holds the same values; placements are mutable, so
     * {@link #equals} stays identity
     */
    public boolean sameAs(HudPlacement other) {
        return other != null && x == other.x && y == other.y && scale == other.scale && enabled == other.enabled
                && removed == other.removed && style.equals(other.style)
                && (group == null ? other.group == null : group.equals(other.group));
    }

    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
