package net.streamline.client;

import singularity.client.hud.HudBoxStyle;
import singularity.client.hud.HudElement;
import singularity.client.hud.HudGroup;
import singularity.client.hud.HudPlacement;
import singularity.client.hud.HudRegistry;
import singularity.client.ClientEnvironment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the registered HUD elements where the player placed them.
 *
 * <p>Elements come from modules, so each is drawn inside its own try/catch: one that keeps
 * failing is logged a few times and then skipped until it is registered again, rather than
 * flooding the log every frame or taking the rest of the HUD down with it.</p>
 *
 * <p>Each element's {@link HudBoxStyle frame} is drawn inside its bounds: the background under
 * it and the border over it. A {@link HudGroup}'s frame covers the area of its members that
 * have something to show, padded by {@link #GROUP_PADDING} and kept on screen, with its
 * background under every element and its border over them. An element showing nothing (zero
 * width or height) gets no frame and does not count towards its group's area.</p>
 */
public final class HudOverlay {

    /** Failures before an element is skipped. */
    private static final int MAX_FAILURES = 5;
    /** Pixels a group's frame reaches past its members. */
    public static final int GROUP_PADDING = 2;

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

    /** The on-screen rectangle of one element or group, in GUI-scaled pixels. */
    public static final class Bounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        /** Whether the element showed nothing; its bounds are then a 1x1 placeholder. */
        public final boolean empty;

        public Bounds(int x, int y, int width, int height) {
            this(x, y, width, height, false);
        }

        Bounds(int x, int y, int width, int height, boolean empty) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.empty = empty;
        }

        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    /** Draws every shown element, with the frames of their groups. */
    public static void render(Canvas canvas) {
        List<HudRegistry.Registered> shown = new ArrayList<>();
        for (HudRegistry.Registered registered : HudRegistry.elements()) {
            if (! registered.getPlacement().isEnabled() || isSkipped(registered)) continue;
            try {
                if (registered.getElement().isVisible()) shown.add(registered);
            } catch (Throwable t) {
                fail(registered, t);
            }
        }
        Map<HudGroup, Bounds> groups = groupBounds(canvas, shown, false);
        for (Map.Entry<HudGroup, Bounds> entry : groups.entrySet()) {
            drawFrame(canvas, entry.getKey().getStyle(), entry.getValue(), true, false);
        }
        for (HudRegistry.Registered registered : shown) {
            try {
                draw(canvas, registered, false);
                FAILURES.remove(registered.getKey());
            } catch (Throwable t) {
                fail(registered, t);
            }
        }
        for (Map.Entry<HudGroup, Bounds> entry : groups.entrySet()) {
            drawFrame(canvas, entry.getKey().getStyle(), entry.getValue(), false, true);
        }
    }

    /**
     * The area each group's frame covers: its listed members that show something, padded by
     * {@link #GROUP_PADDING} and kept on screen. Groups with no such member are left out.
     *
     * @param elements the elements being drawn; members not among them do not count
     */
    public static Map<HudGroup, Bounds> groupBounds(Canvas canvas, List<HudRegistry.Registered> elements, boolean preview) {
        Map<HudGroup, int[]> areas = new LinkedHashMap<>();
        for (HudRegistry.Registered registered : elements) {
            HudGroup group = HudRegistry.groupOf(registered);
            if (group == null) continue;
            Bounds b;
            try {
                b = bounds(canvas, registered, preview);
            } catch (Throwable t) {
                fail(registered, t);
                continue;
            }
            if (b.empty) continue;
            int[] area = areas.get(group);
            if (area == null) {
                areas.put(group, new int[] {b.x, b.y, b.x + b.width, b.y + b.height});
            } else {
                area[0] = Math.min(area[0], b.x);
                area[1] = Math.min(area[1], b.y);
                area[2] = Math.max(area[2], b.x + b.width);
                area[3] = Math.max(area[3], b.y + b.height);
            }
        }
        Map<HudGroup, Bounds> result = new LinkedHashMap<>();
        for (Map.Entry<HudGroup, int[]> entry : areas.entrySet()) {
            int[] a = entry.getValue();
            int x1 = Math.max(0, a[0] - GROUP_PADDING);
            int y1 = Math.max(0, a[1] - GROUP_PADDING);
            int x2 = Math.min(canvas.width(), a[2] + GROUP_PADDING);
            int y2 = Math.min(canvas.height(), a[3] + GROUP_PADDING);
            result.put(entry.getKey(), new Bounds(x1, y1, x2 - x1, y2 - y1));
        }
        return result;
    }

    /**
     * Draws parts of a frame inside {@code bounds}: the background and/or the one-pixel border,
     * each only if the style turns it on.
     */
    public static void drawFrame(Canvas canvas, HudBoxStyle style, Bounds bounds, boolean background, boolean border) {
        if (bounds.empty || bounds.width <= 0 || bounds.height <= 0) return;
        int x2 = bounds.x + bounds.width;
        int y2 = bounds.y + bounds.height;
        if (background && style.isBackground()) canvas.fill(bounds.x, bounds.y, x2, y2, Canvas.opaque(style.getBackgroundColor()));
        if (border && style.isBorder()) canvas.outline(bounds.x, bounds.y, x2, y2, Canvas.opaque(style.getBorderColor()));
    }

    /**
     * Where the element sits for its current size, kept fully on screen.
     */
    public static Bounds bounds(Canvas canvas, HudRegistry.Registered registered, boolean preview) {
        HudElement element = registered.getElement();
        HudPlacement placement = registered.getPlacement();
        CanvasHudRenderer renderer = new CanvasHudRenderer(canvas, placement.getStyle().isBackground());
        double scale = placement.getScale();
        int rawWidth = element.width(renderer, preview);
        int rawHeight = element.height(renderer, preview);
        int width = (int) Math.ceil(Math.max(1, rawWidth) * scale);
        int height = (int) Math.ceil(Math.max(1, rawHeight) * scale);
        int x = (int) Math.round(placement.getX() * canvas.width());
        int y = (int) Math.round(placement.getY() * canvas.height());
        x = Math.max(0, Math.min(x, canvas.width() - width));
        y = Math.max(0, Math.min(y, canvas.height() - height));
        return new Bounds(x, y, width, height, rawWidth <= 0 || rawHeight <= 0);
    }

    /**
     * Draws one element at its place, inside its own frame, and returns where that was. Its
     * group's frame is not drawn here.
     */
    public static Bounds draw(Canvas canvas, HudRegistry.Registered registered, boolean preview) {
        Bounds bounds = bounds(canvas, registered, preview);
        HudPlacement placement = registered.getPlacement();
        HudBoxStyle style = placement.getStyle();
        float scale = (float) placement.getScale();
        drawFrame(canvas, style, bounds, true, false);
        canvas.push();
        try {
            canvas.translate(bounds.x, bounds.y);
            canvas.scale(scale);
            registered.getElement().render(new CanvasHudRenderer(canvas, style.isBackground()), preview);
        } finally {
            canvas.pop();
        }
        drawFrame(canvas, style, bounds, false, true);
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
        if (failures.count == 1) {
            ClientEnvironment.logWarning("HUD element '" + registered.getKey() + "' failed to draw", t);
        } else if (failures.count == MAX_FAILURES) {
            ClientEnvironment.logWarning("HUD element '" + registered.getKey() + "' failed to draw: " + t
                    + " (no longer drawn until it is registered again)");
        }
    }
}
