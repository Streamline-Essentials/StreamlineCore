package singularity.gui;

/**
 * How a player clicked a slot of a {@link CosmicGui}, independent of the platform that rendered
 * it. Renderers map their own click kinds onto these; anything without a counterpart is
 * {@link #OTHER}.
 */
public enum GuiClickType {
    LEFT,
    SHIFT_LEFT,
    RIGHT,
    SHIFT_RIGHT,
    MIDDLE,
    NUMBER_KEY,
    DOUBLE_CLICK,
    DROP,
    CONTROL_DROP,
    OTHER,
    ;

    public boolean isLeftClick() {
        return this == LEFT || this == SHIFT_LEFT || this == DOUBLE_CLICK;
    }

    public boolean isRightClick() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    public boolean isShiftClick() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }

    /**
     * Parses a click type by name, falling back to {@link #OTHER} for unknown or missing names.
     */
    public static GuiClickType fromName(String name) {
        if (name == null) return OTHER;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
