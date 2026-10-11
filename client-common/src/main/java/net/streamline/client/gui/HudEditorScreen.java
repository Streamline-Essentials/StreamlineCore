package net.streamline.client.gui;

import net.streamline.client.Canvas;
import net.streamline.client.HudOverlay;
import net.streamline.client.compat.ClientCompat;
import net.streamline.client.compat.CompatScreen;
import singularity.client.hud.HudPlacement;
import singularity.client.hud.HudRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the player arrange the HUD: drag an element to move it, scroll over it to scale it,
 * right-click it to show or hide it, and press R over it to put it back where it started.
 * Hidden elements stay visible here, dimmed, so they can be shown again.
 *
 * <p>The layout is saved when a drag ends, after every other change, and when the editor
 * closes.</p>
 *
 * <p>A drag follows the pointer every frame while the left button is held, read from the
 * window itself, rather than relying on drag and release events: mods can take those before
 * the screen sees them. The events, when they do arrive, only move it sooner.</p>
 */
public final class HudEditorScreen extends CompatScreen {

    private static final String HINT = "Drag to move • Scroll to scale • Right-click to show/hide • R to reset • Esc to finish";
    private static final double SCALE_STEP = 0.1;

    /** The screen to return to: a version-specific screen object, or {@code null}. */
    private final Object parent;

    /** What each element occupied in the last frame, topmost last. */
    private final List<Placed> placed = new ArrayList<>();

    private HudRegistry.Registered dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private int lastWidth = 1;
    private int lastHeight = 1;
    /** The pointer in the last frame; key presses carry no position of their own. */
    private int lastMouseX;
    private int lastMouseY;

    public HudEditorScreen(Object parent) {
        super("Streamline HUD Editor");
        this.parent = parent;
    }

    private static final class Placed {
        final HudRegistry.Registered registered;
        final HudOverlay.Bounds bounds;

        Placed(HudRegistry.Registered registered, HudOverlay.Bounds bounds) {
            this.registered = registered;
            this.bounds = bounds;
        }
    }

    @Override
    protected void draw(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastWidth = Math.max(1, canvas.width());
        lastHeight = Math.max(1, canvas.height());
        canvas.fill(0, 0, canvas.width(), canvas.height(), 0x50000000);
        // Centre guides make lining elements up easier.
        canvas.fill(canvas.width() / 2, 0, canvas.width() / 2 + 1, canvas.height(), 0x20FFFFFF);
        canvas.fill(0, canvas.height() / 2, canvas.width(), canvas.height() / 2 + 1, 0x20FFFFFF);

        if (dragging != null) {
            if (ClientCompat.leftMouseDown()) dragTo(mouseX, mouseY);
            else endDrag();
        }

        placed.clear();
        for (HudRegistry.Registered registered : HudRegistry.elements()) {
            try {
                HudOverlay.Bounds bounds = HudOverlay.draw(canvas, registered, true);
                placed.add(new Placed(registered, bounds));
            } catch (Throwable t) {
                HudOverlay.fail(registered, t);
            }
        }

        Placed hovered = dragging != null ? find(dragging) : hoveredAt(mouseX, mouseY);
        for (Placed entry : placed) {
            HudOverlay.Bounds b = entry.bounds;
            boolean shown = entry.registered.getPlacement().isEnabled();
            if (! shown) canvas.fill(b.x, b.y, b.x + b.width, b.y + b.height, 0xB0000000);
            int border = entry == hovered ? (dragging != null ? Theme.ACCENT_LIGHT : Theme.ACCENT) : (shown ? 0x80FFFFFF : 0x60FF5555);
            canvas.outline(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, border);
        }

        if (hovered != null) {
            HudPlacement placement = hovered.registered.getPlacement();
            String label = hovered.registered.getElement().getDisplayName()
                    + String.format("  %.2fx", placement.getScale())
                    + (placement.isEnabled() ? "" : "  (hidden)");
            int labelY = hovered.bounds.y >= 12 ? hovered.bounds.y - 11 : hovered.bounds.y + hovered.bounds.height + 2;
            int labelX = Math.max(2, Math.min(hovered.bounds.x, canvas.width() - canvas.textWidth(label) - 4));
            canvas.fill(labelX - 2, labelY - 1, labelX + canvas.textWidth(label) + 2, labelY + canvas.lineHeight(), 0xC0000000);
            canvas.text(label, labelX, labelY, Theme.TEXT);
        }

        if (placed.isEmpty()) {
            String empty = "No HUD elements are registered. Install a client module that adds some, such as StreamlineHUD.";
            int y = canvas.height() / 2 - 20;
            for (String line : Theme.wrap(canvas, empty, Math.min(300, canvas.width() - 20))) {
                canvas.text(line, (canvas.width() - canvas.textWidth(line)) / 2, y, Theme.TEXT);
                y += canvas.lineHeight() + 2;
            }
        }

        String hint = Theme.fit(canvas, HINT, canvas.width() - 8);
        int hintX = (canvas.width() - canvas.textWidth(hint)) / 2;
        int hintY = canvas.height() - canvas.lineHeight() - 4;
        canvas.fill(hintX - 3, hintY - 2, hintX + canvas.textWidth(hint) + 3, hintY + canvas.lineHeight() + 1, 0xA0000000);
        canvas.text(hint, hintX, hintY, Theme.MUTED);
    }

    private Placed hoveredAt(double x, double y) {
        for (int i = placed.size() - 1; i >= 0; i--) {
            if (placed.get(i).bounds.contains(x, y)) return placed.get(i);
        }
        return null;
    }

    private Placed find(HudRegistry.Registered registered) {
        for (Placed entry : placed) {
            if (entry.registered.getKey().equals(registered.getKey())) return entry;
        }
        return null;
    }

    @Override
    protected boolean mouseDown(double x, double y, int button) {
        Placed hit = hoveredAt(x, y);
        if (hit == null) return false;
        if (button == 0) {
            dragging = hit.registered;
            dragOffsetX = x - hit.bounds.x;
            dragOffsetY = y - hit.bounds.y;
            return true;
        }
        if (button == 1) {
            HudPlacement placement = hit.registered.getPlacement();
            placement.setEnabled(! placement.isEnabled());
            HudRegistry.save();
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseDrag(double x, double y, int button, double dx, double dy) {
        if (dragging == null) return false;
        dragTo(x, y);
        return true;
    }

    /** Moves the dragged element so the point grabbed stays under the pointer. */
    private void dragTo(double x, double y) {
        Placed current = find(dragging);
        int width = current == null ? 0 : current.bounds.width;
        int height = current == null ? 0 : current.bounds.height;
        double left = Math.max(0, Math.min(x - dragOffsetX, lastWidth - width));
        double top = Math.max(0, Math.min(y - dragOffsetY, lastHeight - height));
        dragging.getPlacement().moveTo(left / lastWidth, top / lastHeight);
    }

    private void endDrag() {
        dragging = null;
        HudRegistry.save();
    }

    @Override
    protected boolean mouseUp(double x, double y, int button) {
        if (dragging == null) return false;
        endDrag();
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        Placed hit = hoveredAt(x, y);
        if (hit == null || amount == 0) return false;
        HudPlacement placement = hit.registered.getPlacement();
        double scale = Math.round((placement.getScale() + Math.signum(amount) * SCALE_STEP) * 20.0) / 20.0;
        placement.scaleTo(scale);
        HudRegistry.save();
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (key != Theme.GLFW_R) return false;
        Placed hit = hoveredAt(lastMouseX, lastMouseY);
        if (hit == null) return false;
        HudRegistry.reset(hit.registered.getOwner(), hit.registered.getElement());
        HudRegistry.save();
        return true;
    }

    @Override
    public void onClose() {
        HudRegistry.save();
        ClientCompat.setScreen(parent);
    }
}
