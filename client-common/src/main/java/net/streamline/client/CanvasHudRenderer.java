package net.streamline.client;

import net.minecraft.network.chat.Component;
import net.streamline.platform.text.LegacyText;
import singularity.client.hud.HudRenderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link HudRenderer} over a {@link Canvas}. Text goes through {@link LegacyText}; parsed
 * lines are cached, since most HUD text repeats from frame to frame.
 */
public final class CanvasHudRenderer implements HudRenderer {

    private static final int CACHE_LIMIT = 512;
    private static final Map<String, Component> PARSED = new ConcurrentHashMap<>();

    private final Canvas canvas;

    public CanvasHudRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    static Component parse(String text) {
        if (text == null) return Component.empty();
        Component cached = PARSED.get(text);
        if (cached != null) return cached;
        if (PARSED.size() >= CACHE_LIMIT) PARSED.clear();
        Component parsed = LegacyText.parse(text);
        PARSED.put(text, parsed);
        return parsed;
    }

    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        canvas.text(parse(text), x, y, Canvas.opaque(color), shadow);
    }

    @Override
    public int textWidth(String text) {
        return canvas.textWidth(parse(text));
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
