package net.streamline.client;

/**
 * Drawing on the game's GUI, the same on every Minecraft version. Each version's
 * {@code GfxCanvas} implements it over that version's graphics object.
 *
 * <p>Coordinates are GUI-scaled pixels. Colors are ARGB; {@link #opaque(int)} makes a color
 * without alpha fully opaque, since from 1.21.6 a zero alpha draws nothing.</p>
 *
 * <p>Text is a string with {@code &} or {@code §} color codes and {@code {#rrggbb}} /
 * {@code &#rrggbb} hex colors, so nothing here depends on a Minecraft text type. A version
 * without hex colors draws them as the nearest legacy color.</p>
 */
public interface Canvas {

    /** @return the GUI-scaled screen width */
    int width();

    /** @return the GUI-scaled screen height */
    int height();

    void fill(int x1, int y1, int x2, int y2, int color);

    /** @param color the color of text without a color code of its own */
    void text(String text, int x, int y, int color, boolean shadow);

    /** @return the width of {@code text} in pixels, color codes excluded */
    int textWidth(String text);

    /** Draws {@code text} as typed, with color codes shown rather than applied. */
    void rawText(String text, int x, int y, int color);

    /** @return the width of {@code text} drawn by {@link #rawText} */
    int rawTextWidth(String text);

    int lineHeight();

    /** Saves the current transform; pair with {@link #pop()}. */
    void push();

    void pop();

    void translate(float x, float y);

    void scale(float factor);

    /** Restricts drawing to a rectangle, in screen coordinates, until {@link #endClip()}. */
    void clip(int x1, int y1, int x2, int y2);

    void endClip();

    /** @return {@code color} with full alpha if it has none */
    static int opaque(int color) {
        return (color >>> 24) == 0 ? color | 0xFF000000 : color;
    }

    default void text(String text, int x, int y, int color) {
        text(text, x, y, color, false);
    }

    /** Draws a one-pixel outline inside the rectangle. */
    default void outline(int x1, int y1, int x2, int y2, int color) {
        fill(x1, y1, x2, y1 + 1, color);
        fill(x1, y2 - 1, x2, y2, color);
        fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }
}
