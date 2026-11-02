package net.streamline.client;

import singularity.client.color.Chroma;
import singularity.client.color.TextColors;
import singularity.client.hud.HudRenderer;
import singularity.client.settings.StreamlineClientSettings;

/**
 * {@link HudRenderer} over a {@link Canvas}, which takes the same color-coded strings. Only HUD
 * elements draw through it, so the Client Settings it applies never touch the menus.
 */
public final class CanvasHudRenderer implements HudRenderer {

    private static final int WHITE = 0xFFFFFFFF;

    private final Canvas canvas;
    private final boolean playerBackground;

    public CanvasHudRenderer(Canvas canvas) {
        this(canvas, false);
    }

    /** @param playerBackground whether the core draws the player's own background under the element */
    public CanvasHudRenderer(Canvas canvas, boolean playerBackground) {
        this.canvas = canvas;
        this.playerBackground = playerBackground;
    }

    @Override
    public boolean hasPlayerBackground() {
        return playerBackground;
    }

    /**
     * Draws HUD text under the Client Settings that cover every element: Text Shadow only allows
     * a shadow the element asked for, Strip Colors draws it plain white, and Chroma Everywhere
     * draws all of it in the chroma color.
     */
    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        String shown = text == null ? "" : text;
        int base = Canvas.opaque(color);
        if (StreamlineClientSettings.chromaEverywhere()) {
            shown = TextColors.recolor(shown, Chroma.code());
        } else if (StreamlineClientSettings.stripColors()) {
            shown = TextColors.strip(shown);
            base = WHITE;
        }
        canvas.text(shown, x, y, base, shadow && StreamlineClientSettings.textShadow());
    }

    @Override
    public int textWidth(String text) {
        return canvas.textWidth(text == null ? "" : text);
    }

    @Override
    public int lineHeight() {
        return canvas.lineHeight();
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        // 0 is "no fill" for HUD backgrounds rather than opaque black.
        if (color == 0) return;
        canvas.fill(x1, y1, x2, y2, Canvas.opaque(color));
    }
}
