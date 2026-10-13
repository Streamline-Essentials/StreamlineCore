package singularity.client.hud;

import lombok.Getter;
import lombok.Setter;

/**
 * The frame the player put around a HUD element or group: an optional background and an
 * optional one-pixel border, each with its own ARGB color.
 *
 * <p>Whether each part is drawn is its own flag, since a color with no alpha bits draws fully
 * opaque. The frame is drawn inside the element's bounds, so it never changes where the element
 * fits on screen.</p>
 */
@Getter
@Setter
public final class HudBoxStyle {

    public static final int DEFAULT_BACKGROUND_COLOR = 0x60000000;
    public static final int DEFAULT_BORDER_COLOR = 0xFFFFFFFF;

    private volatile boolean background;
    private volatile int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private volatile boolean border;
    private volatile int borderColor = DEFAULT_BORDER_COLOR;

    public HudBoxStyle() {
    }

    public HudBoxStyle(boolean background, int backgroundColor, boolean border, int borderColor) {
        this.background = background;
        this.backgroundColor = backgroundColor;
        this.border = border;
        this.borderColor = borderColor;
    }

    public HudBoxStyle copy() {
        return new HudBoxStyle(background, backgroundColor, border, borderColor);
    }

    public void copyFrom(HudBoxStyle other) {
        background = other.background;
        backgroundColor = other.backgroundColor;
        border = other.border;
        borderColor = other.borderColor;
    }

    /** @return whether anything is drawn */
    public boolean isVisible() {
        return background || border;
    }

    /** @return {@code background,backgroundColor,border,borderColor}, colors as 8-digit hex */
    public String serialize() {
        return background + "," + hex(backgroundColor) + "," + border + "," + hex(borderColor);
    }

    /**
     * Reads four fields written by {@link #serialize()} starting at {@code from}; missing or
     * unreadable fields keep their defaults.
     */
    public static HudBoxStyle parse(String[] parts, int from) {
        HudBoxStyle style = new HudBoxStyle();
        if (parts.length > from) style.background = Boolean.parseBoolean(parts[from].trim());
        if (parts.length > from + 1) style.backgroundColor = parseColor(parts[from + 1], DEFAULT_BACKGROUND_COLOR);
        if (parts.length > from + 2) style.border = Boolean.parseBoolean(parts[from + 2].trim());
        if (parts.length > from + 3) style.borderColor = parseColor(parts[from + 3], DEFAULT_BORDER_COLOR);
        return style;
    }

    private static String hex(int argb) {
        String hex = Integer.toHexString(argb);
        StringBuilder out = new StringBuilder(8);
        for (int i = hex.length(); i < 8; i++) out.append('0');
        return out.append(hex).toString();
    }

    private static int parseColor(String text, int fallback) {
        try {
            // Unsigned, since colors with alpha above 7F overflow a signed int.
            return (int) Long.parseLong(text.trim(), 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof HudBoxStyle)) return false;
        HudBoxStyle other = (HudBoxStyle) o;
        return background == other.background && backgroundColor == other.backgroundColor
                && border == other.border && borderColor == other.borderColor;
    }

    @Override
    public int hashCode() {
        return ((Boolean.hashCode(background) * 31 + backgroundColor) * 31 + Boolean.hashCode(border)) * 31 + borderColor;
    }
}
