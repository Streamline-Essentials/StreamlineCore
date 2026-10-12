package net.streamline.client;

import singularity.client.hud.HudRenderer;

/**
 * {@link HudRenderer} over a {@link Canvas}, which takes the same color-coded strings.
 */
public final class CanvasHudRenderer implements HudRenderer {

    private final Canvas canvas;

    public CanvasHudRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        canvas.text(text == null ? "" : text, x, y, Canvas.opaque(color), shadow);
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
