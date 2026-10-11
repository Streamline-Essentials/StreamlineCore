package net.streamline.client;

import singularity.client.hud.HudElement;
import singularity.client.hud.HudPlacement;
import singularity.client.hud.HudRegistry;
import singularity.client.ClientEnvironment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the registered HUD elements where the player placed them.
 *
 * <p>Elements come from modules, so each is drawn inside its own try/catch: one that keeps
 * failing is logged a few times and then skipped until it is registered again, rather than
 * flooding the log every frame or taking the rest of the HUD down with it.</p>
 */
public final class HudOverlay {

    /** Failures before an element is skipped. */
    private static final int MAX_FAILURES = 5;

    /** Consecutive failures by element key, against the element instance that failed. */
    private static final Map<String, Failures> FAILURES = new ConcurrentHashMap<>();

    private HudOverlay() {}

    private static final class Failures {
        final HudElement element;
        int count;

        Failures(HudElement element) {
            this.element = element;
        }
    }

    /** The on-screen rectangle of one element, in GUI-scaled pixels. */
    public static final class Bounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        Bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    /** Draws every shown element. */
    public static void render(Canvas canvas) {
        for (HudRegistry.Registered registered : HudRegistry.elements()) {
            HudPlacement placement = registered.getPlacement();
            if (! placement.isEnabled() || isSkipped(registered)) continue;
            try {
                if (! registered.getElement().isVisible()) continue;
                draw(canvas, registered, false);
                FAILURES.remove(registered.getKey());
            } catch (Throwable t) {
                fail(registered, t);
            }
        }
    }

    /**
     * Where the element sits for its current size, kept fully on screen.
     */
    public static Bounds bounds(Canvas canvas, HudRegistry.Registered registered, boolean preview) {
        HudElement element = registered.getElement();
        HudPlacement placement = registered.getPlacement();
        CanvasHudRenderer renderer = new CanvasHudRenderer(canvas);
        double scale = placement.getScale();
        int width = (int) Math.ceil(Math.max(1, element.width(renderer, preview)) * scale);
        int height = (int) Math.ceil(Math.max(1, element.height(renderer, preview)) * scale);
        int x = (int) Math.round(placement.getX() * canvas.width());
        int y = (int) Math.round(placement.getY() * canvas.height());
        x = Math.max(0, Math.min(x, canvas.width() - width));
        y = Math.max(0, Math.min(y, canvas.height() - height));
        return new Bounds(x, y, width, height);
    }

    /**
     * Draws one element at its place and returns where that was.
     */
    public static Bounds draw(Canvas canvas, HudRegistry.Registered registered, boolean preview) {
        Bounds bounds = bounds(canvas, registered, preview);
        float scale = (float) registered.getPlacement().getScale();
        canvas.push();
        try {
            canvas.translate(bounds.x, bounds.y);
            canvas.scale(scale);
            registered.getElement().render(new CanvasHudRenderer(canvas), preview);
        } finally {
            canvas.pop();
        }
        return bounds;
    }

    private static boolean isSkipped(HudRegistry.Registered registered) {
        Failures failures = FAILURES.get(registered.getKey());
        if (failures == null) return false;
        // A module that registers its element again gets a fresh start.
        if (failures.element != registered.getElement()) {
            FAILURES.remove(registered.getKey());
            return false;
        }
        return failures.count >= MAX_FAILURES;
    }

    /** Records a failed draw, logging the first and the one after which the element is skipped. */
    public static void fail(HudRegistry.Registered registered, Throwable t) {
        Failures failures = FAILURES.computeIfAbsent(registered.getKey(), key -> new Failures(registered.getElement()));
        failures.count++;
        if (failures.count == 1 || failures.count == MAX_FAILURES) {
            ClientEnvironment.logWarning("HUD element '" + registered.getKey() + "' failed to draw: " + t
                    + (failures.count >= MAX_FAILURES ? " (no longer drawn until it is registered again)" : ""));
        }
    }
}
