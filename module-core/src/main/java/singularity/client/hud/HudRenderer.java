package singularity.client.hud;

import singularity.client.settings.StreamlineClientSettings;

/**
 * Draws a {@link HudElement}. Coordinates are GUI-scaled pixels relative to the element's
 * top-left corner; the renderer has already moved and scaled to the element's place.
 * Colors are ARGB, and a color with no alpha bits set is drawn fully opaque.
 */
public interface HudRenderer {

    /**
     * Draws text with {@code &} color codes and {@code {#rrggbb}} hex colors.
     *
     * @param color the color of text without a color code of its own
     */
    void text(String text, int x, int y, int color, boolean shadow);

    /** @return the width of {@code text} in pixels, color codes excluded */
    int textWidth(String text);

    /** @return the height of one line of text in pixels */
    int lineHeight();

    /** Fills the rectangle from ({@code x1}, {@code y1}) to ({@code x2}, {@code y2}), exclusive. */
    void fill(int x1, int y1, int x2, int y2, int color);

    /**
     * @return whether the player gave this element a background of their own in the HUD editor,
     * which an element drawing its own background should then leave out
     */
    default boolean hasPlayerBackground() {
        return false;
    }

    /**
     * @return whether the player allows HUD elements to draw their own backgrounds (Client
     * Settings); an element drawing one should leave it out when this is false
     */
    default boolean backgroundsAllowed() {
        return StreamlineClientSettings.background();
    }

    /** @return the ARGB background chosen in Client Settings, for elements with no color of their own */
    default int backgroundColor() {
        return StreamlineClientSettings.backgroundColor();
    }
}
