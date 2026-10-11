package net.streamline.client;

import net.minecraft.network.chat.Component;

/**
 * Drawing on the game's GUI, the same on every Minecraft version. Each version's
 * {@code GfxCanvas} implements it over that version's graphics object.
 *
 * <p>Coordinates are GUI-scaled pixels. Colors are ARGB; {@link #opaque(int)} makes a color
 * without alpha fully opaque, since from 1.21.6 a zero alpha draws nothing.</p>
 */
public interface Canvas {

    /** @return the GUI-scaled screen width */
    int width();

    /** @return the GUI-scaled screen height */
    int height();

    void fill(int x1, int y1, int x2, int y2, int color);

    void text(Component text, int x, int y, int color, boolean shadow);

    int textWidth(Component text);

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

    default void text(String plain, int x, int y, int color) {
        text(Component.literal(plain), x, y, color, false);
    }

    default int textWidth(String plain) {
        return textWidth(Component.literal(plain));
    }

    /** Draws a one-pixel outline inside the rectangle. */
    default void outline(int x1, int y1, int x2, int y2, int color) {
        fill(x1, y1, x2, y1 + 1, color);
        fill(x1, y2 - 1, x2, y2, color);
        fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }
}
