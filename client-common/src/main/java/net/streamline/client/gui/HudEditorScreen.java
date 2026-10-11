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
 * <p>A drag starts on the press the screen receives and then follows the pointer every frame,
 * rather than relying on drag events: the game sends those only while its own record of the held
 * button says so, and mods can take or rewrite that record. It ends on the release event, or
 * once {@link ClientCompat#leftMouseDown()} reports the button up, which is trusted only after it
 * has reported it down during the same drag, so a query that cannot see the button never ends a
 * drag on its own.</p>
 *
 * <p>The button at the top opens the StreamlineClient Menu; it is faint until hovered so it does
 * not hide the HUD behind it.</p>
 */
public final class HudEditorScreen extends CompatScreen {

    private static final String HINT = "Drag to move • Scroll to scale • Right-click to show/hide • R to reset • Esc to finish";
    private static final double SCALE_STEP = 0.1;
    private static final String MENU_LABEL = "StreamlineClient Menu";
    private static final int MENU_PAD_X = 6;
    private static final int MENU_PAD_Y = 4;
    private static final int MENU_TOP = 4;

    /** The screen to return to: a version-specific screen object, or {@code null}. */
    private final Object parent;

    /** What each element occupied in the last frame, topmost last. */
    private final List<Placed> placed = new ArrayList<>();

    private HudRegistry.Registered dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    /** Whether {@link ClientCompat#leftMouseDown()} has seen the button held during this drag. */
    private boolean dragSeenDown;
    /** The menu button's area in the last frame: x1, y1, x2, y2. */
    private final int[] menuButton = new int[4];
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
            if (ClientCompat.leftMouseDown()) {
                dragSeenDown = true;
                dragTo(mouseX, mouseY);
            } else if (dragSeenDown) {
                endDrag();
            } else {
                dragTo(mouseX, mouseY);
            }
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

        drawMenuButton(canvas, mouseX, mouseY);

        String hint = Theme.fit(canvas, HINT, canvas.width() - 8);
        int hintX = (canvas.width() - canvas.textWidth(hint)) / 2;
        int hintY = canvas.height() - canvas.lineHeight() - 4;
        canvas.fill(hintX - 3, hintY - 2, hintX + canvas.textWidth(hint) + 3, hintY + canvas.lineHeight() + 1, 0xA0000000);
        canvas.text(hint, hintX, hintY, Theme.MUTED);
    }

    /** The button that opens the StreamlineClient Menu: faint until the pointer is over it. */
    private void drawMenuButton(Canvas canvas, int mouseX, int mouseY) {
        int width = canvas.textWidth(MENU_LABEL) + MENU_PAD_X * 2;
        int height = canvas.lineHeight() + MENU_PAD_Y * 2;
        menuButton[0] = (canvas.width() - width) / 2;
        menuButton[1] = MENU_TOP;
        menuButton[2] = menuButton[0] + width;
        menuButton[3] = MENU_TOP + height;
        boolean hover = dragging == null && overMenuButton(mouseX, mouseY);
        int fill = hover ? Theme.BUTTON_HOVER : faint(Theme.BUTTON);
        canvas.fill(menuButton[0], menuButton[1], menuButton[2], menuButton[3], fill);
        canvas.outline(menuButton[0], menuButton[1], menuButton[2], menuButton[3],
                hover ? Theme.ACCENT_LIGHT : faint(Theme.CONTROL_BORDER));
        canvas.text(MENU_LABEL, menuButton[0] + MENU_PAD_X, menuButton[1] + MENU_PAD_Y,
                hover ? Theme.TEXT : faint(Theme.TEXT));
    }

    /** @return {@code argb} at half its opacity */
    private static int faint(int argb) {
        return (((argb >>> 24) / 2) << 24) | (argb & 0xFFFFFF);
    }

    private boolean overMenuButton(double x, double y) {
        return x >= menuButton[0] && y >= menuButton[1] && x < menuButton[2] && y < menuButton[3];
    }

    /**
     * Opens the StreamlineClient Menu in place of the editor, going back to the menu the editor
     * was opened from rather than stacking another one on it.
     */
    private void openMenu() {
        HudRegistry.save();
        ClientCompat.setScreen(parent instanceof MenuScreen ? parent : new MenuScreen(parent));
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
        // A press while dragging means the release never arrived; that drag is over.
        if (dragging != null) endDrag();
        if (button == 0 && overMenuButton(x, y)) {
            openMenu();
            return true;
        }
        Placed hit = hoveredAt(x, y);
        if (hit == null) return false;
        if (button == 0) {
            dragging = hit.registered;
            dragSeenDown = false;
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
        dragSeenDown = false;
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
