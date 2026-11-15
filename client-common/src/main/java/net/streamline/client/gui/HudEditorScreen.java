package net.streamline.client.gui;

import net.streamline.client.Canvas;
import net.streamline.client.HudOverlay;
import net.streamline.client.compat.ClientCompat;
import net.streamline.client.compat.CompatScreen;
import singularity.client.hud.HudGroup;
import singularity.client.hud.HudHistory;
import singularity.client.hud.HudPlacement;
import singularity.client.hud.HudRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lets the player arrange the HUD: drag an element to move it, scroll over it to scale it,
 * right-click it to show or hide it, and press R over it to put it back where it started.
 * Hidden elements stay visible here, dimmed, so they can be shown again.
 *
 * <p>Clicking an element selects it: the arrow keys then nudge it a few pixels, or one pixel with
 * Ctrl held, and Delete removes it, which hides it and takes it out of the editor until it is
 * shown again from its module's page in the StreamlineClient Menu. Holding left Shift previews
 * the HUD as it will look, without the menu button or hidden elements.</p>
 *
 * <p>{@link HudGroup Groups} are drawn as the HUD draws them, their background under the
 * elements and their border over them, with a dashed outline of their own here. Pressing
 * anywhere in a group's area, gaps between members included, acts on the whole group: dragging,
 * nudging, scaling (each member's offset from the group's corner scales with it, so the group
 * keeps its shape), showing or hiding, resetting and removing. A group moves only as far as its
 * most blocked member can, so no member is pushed off screen. Ctrl+click adds elements to the
 * selection (a grouped one brings its whole group); G ties two or more selected elements into a
 * new group and U dissolves the selected group.</p>
 *
 * <p>E over an element or group, or a double-click on it, opens {@link HudStylePopup} to edit its
 * frame; while the popup is open it gets every input, and Escape closes it before the editor.
 * Ctrl+Z undoes a change and Ctrl+Y or Ctrl+Shift+Z redoes it ({@link HudHistory}).</p>
 *
 * <p>The layout is saved when a drag ends, after every other change, and when the editor
 * closes.</p>
 *
 * <p>A drag starts on the press the screen receives and then follows the pointer every frame,
 * rather than relying on drag events: the game sends those only while its own record of the held
 * button says so, and mods can take or rewrite that record. It ends on the release event, or
 * once {@link ClientCompat#leftMouseDown()} reports the button up, which is trusted only after it
 * has reported it down during the same drag, so a query that cannot see the button never ends a
 * drag on its own. The layout is recorded for undo once the pointer first moves, so a plain click
 * leaves the undo and redo steps alone.</p>
 *
 * <p>The button at the top opens the StreamlineClient Menu; it is faint until hovered so it does
 * not hide the HUD behind it.</p>
 */
public final class HudEditorScreen extends CompatScreen {

    private static final String HINT = "Drag: move • Scroll: scale • Right-click: show/hide • R: reset • Esc: done";
    private static final String HINT_SELECTED = "Arrows: nudge (Ctrl: 1px) • Delete: remove • E/double-click: frame";
    private static final String HINT_GROUPS = "Ctrl+click: multi-select • G: group • U: ungroup • Ctrl+Z/Y: undo/redo • Left Shift: preview";
    /** GUI pixels an arrow key moves the selection; {@link #NUDGE_FINE} with Ctrl. */
    private static final int NUDGE = 4;
    private static final int NUDGE_FINE = 1;
    private static final double SCALE_STEP = 0.1;
    /** Longest gap between two clicks on the same element or group that opens its frame popup. */
    private static final long DOUBLE_CLICK_MS = 400;
    /** Length of each dash, and of each gap, in a group's outline. */
    private static final int DASH = 3;
    private static final int GROUP_OUTLINE = 0xC05A9BF0;
    private static final String MENU_LABEL = "StreamlineClient Menu";
    private static final int MENU_PAD_X = 6;
    private static final int MENU_PAD_Y = 4;
    private static final int MENU_TOP = 4;
    /** Key names of the keys that make a click add to the selection: Ctrl, or Cmd on macOS. */
    private static final String[] SHORTCUT_KEYS = {
            "key.keyboard.left.control", "key.keyboard.right.control", "key.keyboard.left.win", "key.keyboard.right.win"
    };

    /** The screen to return to: a version-specific screen object, or {@code null}. */
    private final Object parent;

    /** What each element occupied in the last frame, topmost last. */
    private final List<Placed> placed = new ArrayList<>();
    /** The area of each group with something drawn in the last frame, by group id. */
    private final Map<String, HudOverlay.Bounds> groupAreas = new LinkedHashMap<>();

    /** The elements being dragged with where each started, or {@code null} when nothing is. */
    private List<Member> dragging;
    /** The id of the group being dragged, or {@code null} when it is a single element or nothing. */
    private String dragGroup;
    /** The pointer when the drag began, floored to whole pixels. */
    private double dragStartX;
    private double dragStartY;
    /** Whether the layout before this drag has been recorded for undo. */
    private boolean dragRecorded;
    /** Whether {@link ClientCompat#leftMouseDown()} has seen the button held during this drag. */
    private boolean dragSeenDown;

    /** The elements the arrow keys, Delete and G act on, by key, in the order they were picked. */
    private final Set<String> selectedKeys = new LinkedHashSet<>();
    /** The group the arrow keys, Delete and U act on, or {@code null}; never set with {@link #selectedKeys}. */
    private String selectedGroup;
    /** Whether left Shift was held this frame: the editor then shows only what the HUD will. */
    private boolean previewing;
    /** The frame popup, open over the editor, or {@code null}. */
    private HudStylePopup popup;
    /**
     * Whether the popup was just opened by the E key, whose character event follows the key
     * event on modern versions and must not reach the popup as typing. Cleared every frame.
     */
    private boolean swallowE;

    /** What the last left click without Ctrl landed on ({@link Hit#id()}), and when. */
    private String lastClickTarget;
    private long lastClickTime;

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

    /** An element being moved, with the drawn top-left corner and size it had when the move began. */
    private static final class Member {
        final HudRegistry.Registered registered;
        final int x;
        final int y;
        final int width;
        final int height;

        Member(Placed placed) {
            this.registered = placed.registered;
            this.x = placed.bounds.x;
            this.y = placed.bounds.y;
            this.width = placed.bounds.width;
            this.height = placed.bounds.height;
        }
    }

    /** What the pointer is over: a loose element, or a group. */
    private static final class Hit {
        /** The element, or {@code null} for a group. */
        final Placed element;
        /** The group's id, or {@code null} for a loose element. */
        final String group;
        final HudOverlay.Bounds bounds;

        Hit(Placed element, String group, HudOverlay.Bounds bounds) {
            this.element = element;
            this.group = group;
            this.bounds = bounds;
        }

        /** @return an id telling this hit apart from any other element or group */
        String id() {
            return group != null ? "g:" + group : "e:" + element.registered.getKey();
        }
    }

    @Override
    protected void draw(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        swallowE = false;
        lastWidth = Math.max(1, canvas.width());
        lastHeight = Math.max(1, canvas.height());
        previewing = dragging == null && popup == null && ClientCompat.leftShiftDown();
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

        List<HudRegistry.Registered> drawn = new ArrayList<>();
        for (HudRegistry.Registered registered : HudRegistry.elements()) {
            HudPlacement placement = registered.getPlacement();
            if (placement.isRemoved() || (previewing && ! placement.isEnabled())) continue;
            drawn.add(registered);
        }

        // The HUD's own order: group backgrounds, then the elements, then group borders.
        Map<HudGroup, HudOverlay.Bounds> groups = HudOverlay.groupBounds(canvas, drawn, true);
        groupAreas.clear();
        for (Map.Entry<HudGroup, HudOverlay.Bounds> entry : groups.entrySet()) {
            groupAreas.put(entry.getKey().getId(), entry.getValue());
            HudOverlay.drawFrame(canvas, entry.getKey().getStyle(), entry.getValue(), true, false);
        }
        placed.clear();
        for (HudRegistry.Registered registered : drawn) {
            try {
                HudOverlay.Bounds bounds = HudOverlay.draw(canvas, registered, true);
                placed.add(new Placed(registered, bounds));
            } catch (Throwable t) {
                HudOverlay.fail(registered, t);
            }
        }
        for (Map.Entry<HudGroup, HudOverlay.Bounds> entry : groups.entrySet()) {
            HudOverlay.drawFrame(canvas, entry.getKey().getStyle(), entry.getValue(), false, true);
        }

        // Holding Shift hides hidden elements for a moment; that must not drop the selection.
        if (! previewing) pruneSelection();
        Hit hovered = popup != null ? null : dragging != null ? dragHit() : hitAt(mouseX, mouseY);
        for (Placed entry : placed) {
            HudOverlay.Bounds b = entry.bounds;
            boolean shown = entry.registered.getPlacement().isEnabled();
            if (! shown) canvas.fill(b.x, b.y, b.x + b.width, b.y + b.height, 0xB0000000);
            boolean over = hovered != null && hovered.element == entry;
            int border = over ? (dragging != null ? Theme.ACCENT_LIGHT : Theme.ACCENT) : (shown ? 0x80FFFFFF : 0x60FF5555);
            canvas.outline(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, border);
            if (selectedKeys.contains(entry.registered.getKey())) {
                canvas.outline(b.x - 2, b.y - 2, b.x + b.width + 2, b.y + b.height + 2, Theme.ACCENT_LIGHT);
            }
        }
        for (Map.Entry<String, HudOverlay.Bounds> entry : groupAreas.entrySet()) {
            HudOverlay.Bounds b = entry.getValue();
            boolean over = hovered != null && entry.getKey().equals(hovered.group);
            if (over) {
                canvas.outline(b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, dragging != null ? Theme.ACCENT_LIGHT : Theme.ACCENT);
            } else {
                dashed(canvas, b.x - 1, b.y - 1, b.x + b.width + 1, b.y + b.height + 1, GROUP_OUTLINE);
            }
            if (entry.getKey().equals(selectedGroup)) {
                canvas.outline(b.x - 3, b.y - 3, b.x + b.width + 3, b.y + b.height + 3, Theme.ACCENT_LIGHT);
            }
        }

        if (hovered != null) drawLabel(canvas, hovered);

        if (placed.isEmpty()) {
            String empty = "No HUD elements are registered. Install a client module that adds some, such as StreamlineHUD.";
            int y = canvas.height() / 2 - 20;
            for (String line : Theme.wrap(canvas, empty, Math.min(300, canvas.width() - 20))) {
                canvas.text(line, (canvas.width() - canvas.textWidth(line)) / 2, y, Theme.TEXT);
                y += canvas.lineHeight() + 2;
            }
        }

        if (previewing) return;
        drawMenuButton(canvas, mouseX, mouseY);

        int hintY = canvas.height() - canvas.lineHeight() - 4;
        drawHint(canvas, HINT, hintY);
        drawHint(canvas, HINT_SELECTED, hintY - canvas.lineHeight() - 4);
        drawHint(canvas, HINT_GROUPS, hintY - (canvas.lineHeight() + 4) * 2);

        if (popup != null) {
            popup.draw(canvas, mouseX, mouseY);
            if (popup.isClosed()) closePopup();
        }
    }

    /** The name of what the pointer is over, above it, or below it near the top of the screen. */
    private void drawLabel(Canvas canvas, Hit hovered) {
        String label;
        if (hovered.group != null) {
            HudGroup group = HudRegistry.group(hovered.group);
            List<Placed> members = placedIn(hovered.group);
            boolean anyShown = false;
            for (Placed member : members) anyShown |= member.registered.getPlacement().isEnabled();
            label = (group == null ? "Group" : group.getName()) + "  " + members.size()
                    + (members.size() == 1 ? " element" : " elements") + (anyShown ? "" : "  (hidden)");
        } else {
            HudPlacement placement = hovered.element.registered.getPlacement();
            label = hovered.element.registered.getElement().getDisplayName()
                    + String.format("  %.2fx", placement.getScale())
                    + (placement.isEnabled() ? "" : "  (hidden)");
        }
        HudOverlay.Bounds b = hovered.bounds;
        int labelY = b.y >= 12 ? b.y - 11 : b.y + b.height + 2;
        int labelX = Math.max(2, Math.min(b.x, canvas.width() - canvas.textWidth(label) - 4));
        canvas.fill(labelX - 2, labelY - 1, labelX + canvas.textWidth(label) + 2, labelY + canvas.lineHeight(), 0xC0000000);
        canvas.text(label, labelX, labelY, Theme.TEXT);
    }

    /** A one-pixel outline inside the rectangle, broken into dashes. */
    private static void dashed(Canvas canvas, int x1, int y1, int x2, int y2, int color) {
        for (int x = x1; x < x2; x += DASH * 2) {
            int end = Math.min(x + DASH, x2);
            canvas.fill(x, y1, end, y1 + 1, color);
            canvas.fill(x, y2 - 1, end, y2, color);
        }
        for (int y = y1; y < y2; y += DASH * 2) {
            int end = Math.min(y + DASH, y2);
            canvas.fill(x1, y, x1 + 1, end, color);
            canvas.fill(x2 - 1, y, x2, end, color);
        }
    }

    private static void drawHint(Canvas canvas, String text, int y) {
        String hint = Theme.fit(canvas, text, canvas.width() - 8);
        int x = (canvas.width() - canvas.textWidth(hint)) / 2;
        canvas.fill(x - 3, y - 2, x + canvas.textWidth(hint) + 3, y + canvas.lineHeight() + 1, 0xA0000000);
        canvas.text(hint, x, y, Theme.MUTED);
    }

    /** The button that opens the StreamlineClient Menu: faint until the pointer is over it. */
    private void drawMenuButton(Canvas canvas, int mouseX, int mouseY) {
        int width = canvas.textWidth(MENU_LABEL) + MENU_PAD_X * 2;
        int height = canvas.lineHeight() + MENU_PAD_Y * 2;
        menuButton[0] = (canvas.width() - width) / 2;
        menuButton[1] = MENU_TOP;
        menuButton[2] = menuButton[0] + width;
        menuButton[3] = MENU_TOP + height;
        boolean hover = dragging == null && popup == null && overMenuButton(mouseX, mouseY);
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

    // ---------------------------------------------------------------- finding things

    private Placed hoveredAt(double x, double y) {
        for (int i = placed.size() - 1; i >= 0; i--) {
            if (placed.get(i).bounds.contains(x, y)) return placed.get(i);
        }
        return null;
    }

    /**
     * What a press at this point acts on. Elements are drawn over every group's background, so
     * the topmost element under the pointer decides: a grouped one stands for its group, a loose
     * one for itself. With no element there, a group whose area covers the point is hit, so the
     * gaps between members grab the group too.
     */
    private Hit hitAt(double x, double y) {
        Placed top = hoveredAt(x, y);
        if (top != null) {
            String group = top.registered.getPlacement().getGroup();
            if (group != null && HudRegistry.group(group) != null) {
                HudOverlay.Bounds area = groupAreas.get(group);
                return new Hit(null, group, area != null ? area : top.bounds);
            }
            return new Hit(top, null, top.bounds);
        }
        List<Map.Entry<String, HudOverlay.Bounds>> areas = new ArrayList<>(groupAreas.entrySet());
        for (int i = areas.size() - 1; i >= 0; i--) {
            if (areas.get(i).getValue().contains(x, y)) return new Hit(null, areas.get(i).getKey(), areas.get(i).getValue());
        }
        return null;
    }

    /** @return what is being dragged, as a hit, or {@code null} if it is no longer drawn */
    private Hit dragHit() {
        if (dragGroup != null) {
            HudOverlay.Bounds area = groupAreas.get(dragGroup);
            return area == null ? null : new Hit(null, dragGroup, area);
        }
        Placed current = dragging.isEmpty() ? null : find(dragging.get(0).registered);
        return current == null ? null : new Hit(current, null, current.bounds);
    }

    private Placed find(HudRegistry.Registered registered) {
        return find(registered.getKey());
    }

    private Placed find(String key) {
        for (Placed entry : placed) {
            if (entry.registered.getKey().equals(key)) return entry;
        }
        return null;
    }

    /** @return the members of a group drawn in the last frame */
    private List<Placed> placedIn(String groupId) {
        List<Placed> result = new ArrayList<>();
        for (Placed entry : placed) {
            if (groupId.equals(entry.registered.getPlacement().getGroup())) result.add(entry);
        }
        return result;
    }

    /** @return the members of a group that are in the editor: registered and not removed */
    private static List<HudRegistry.Registered> editableMembers(String groupId) {
        List<HudRegistry.Registered> result = new ArrayList<>();
        for (HudRegistry.Registered registered : HudRegistry.members(groupId)) {
            if (! registered.getPlacement().isRemoved()) result.add(registered);
        }
        return result;
    }

    /** @return the selected elements, or the selected group's members, drawn in the last frame */
    private List<Placed> selection() {
        if (selectedGroup != null) return placedIn(selectedGroup);
        List<Placed> result = new ArrayList<>();
        for (String key : selectedKeys) {
            Placed entry = find(key);
            if (entry != null) result.add(entry);
        }
        return result;
    }

    private static boolean shortcutHeld() {
        for (String key : SHORTCUT_KEYS) {
            if (ClientCompat.keyDown(key)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- selection

    /** Drops selected elements that are no longer drawn and a selected group with none drawn. */
    private void pruneSelection() {
        selectedKeys.removeIf(key -> find(key) == null);
        if (selectedGroup != null && (HudRegistry.group(selectedGroup) == null || placedIn(selectedGroup).isEmpty())) {
            selectedGroup = null;
        }
    }

    /** Drops selection entries the layout no longer has, without waiting for the next frame. */
    private void pruneSelectionToLayout() {
        if (selectedGroup != null && HudRegistry.group(selectedGroup) == null) selectedGroup = null;
        selectedKeys.removeIf(key -> {
            Placed entry = find(key);
            return entry == null || entry.registered.getPlacement().isRemoved();
        });
    }

    private void clearSelection() {
        selectedKeys.clear();
        selectedGroup = null;
    }

    private void select(Hit hit) {
        clearSelection();
        if (hit.group != null) selectedGroup = hit.group;
        else selectedKeys.add(hit.element.registered.getKey());
    }

    /**
     * Adds what was Ctrl+clicked to the selection, or takes it out if it is all selected already.
     * A group stands for its drawn members, and a selected group turns into its members first.
     */
    private void toggleSelection(Hit hit) {
        if (selectedGroup != null) {
            for (Placed member : placedIn(selectedGroup)) selectedKeys.add(member.registered.getKey());
            selectedGroup = null;
        }
        List<String> keys = new ArrayList<>();
        if (hit.group != null) {
            for (Placed member : placedIn(hit.group)) keys.add(member.registered.getKey());
        } else {
            keys.add(hit.element.registered.getKey());
        }
        if (selectedKeys.containsAll(keys)) selectedKeys.removeAll(keys);
        else selectedKeys.addAll(keys);
    }

    // ---------------------------------------------------------------- popup

    private void openPopup(Hit hit) {
        if (dragging != null) endDrag();
        if (hit.group != null) {
            HudGroup group = HudRegistry.group(hit.group);
            if (group == null) return;
            popup = HudStylePopup.forGroup(group).onTextInput(focused -> textInput(focused));
        } else {
            popup = HudStylePopup.forElement(hit.element.registered).onTextInput(focused -> textInput(focused));
        }
    }

    private void closePopup() {
        popup = null;
        textInput(false);
        HudRegistry.save();
    }

    /** Drops the popup once it has closed itself. */
    private void checkPopup() {
        if (popup != null && popup.isClosed()) closePopup();
    }

    // ---------------------------------------------------------------- mouse

    @Override
    protected boolean mouseDown(double x, double y, int button) {
        if (popup != null) {
            popup.mouseDown(x, y, button);
            checkPopup();
            return true;
        }
        // A press while dragging means the release never arrived; that drag is over.
        if (dragging != null) endDrag();
        if (button == 0 && ! previewing && overMenuButton(x, y)) {
            openMenu();
            return true;
        }
        boolean multi = button == 0 && shortcutHeld();
        Hit hit = hitAt(x, y);
        if (hit == null) {
            if (button == 0 && ! multi) clearSelection();
            return false;
        }
        if (button == 0) {
            if (multi) {
                toggleSelection(hit);
                lastClickTarget = null;
                return true;
            }
            long now = System.currentTimeMillis();
            if (hit.id().equals(lastClickTarget) && now - lastClickTime <= DOUBLE_CLICK_MS) {
                lastClickTarget = null;
                select(hit);
                openPopup(hit);
                return true;
            }
            lastClickTarget = hit.id();
            lastClickTime = now;
            select(hit);
            startDrag(hit, x, y);
            return true;
        }
        if (button == 1) {
            HudHistory.record();
            if (hit.group != null) {
                List<HudRegistry.Registered> members = editableMembers(hit.group);
                boolean anyOff = false;
                for (HudRegistry.Registered member : members) anyOff |= ! member.getPlacement().isEnabled();
                for (HudRegistry.Registered member : members) member.getPlacement().setEnabled(anyOff);
            } else {
                HudPlacement placement = hit.element.registered.getPlacement();
                placement.setEnabled(! placement.isEnabled());
            }
            HudRegistry.save();
            return true;
        }
        return false;
    }

    private void startDrag(Hit hit, double x, double y) {
        List<Member> members = new ArrayList<>();
        if (hit.group != null) {
            for (Placed member : placedIn(hit.group)) members.add(new Member(member));
        } else {
            members.add(new Member(hit.element));
        }
        if (members.isEmpty()) return;
        dragging = members;
        dragGroup = hit.group;
        dragStartX = Math.floor(x);
        dragStartY = Math.floor(y);
        dragRecorded = false;
        dragSeenDown = false;
    }

    @Override
    protected boolean mouseDrag(double x, double y, int button, double dx, double dy) {
        if (popup != null) return true;
        if (dragging == null) return false;
        dragTo(x, y);
        return true;
    }

    /**
     * Moves what is dragged so the point grabbed stays under the pointer, as far as the screen
     * allows. The pointer arrives in whole pixels each frame but as fractions in drag events, so
     * both are floored, giving every frame the same whole-pixel offset.
     */
    private void dragTo(double x, double y) {
        x = Math.floor(x);
        y = Math.floor(y);
        if (! dragRecorded) {
            if (x == dragStartX && y == dragStartY) return;
            HudHistory.record();
            dragRecorded = true;
        }
        move(dragging, x - dragStartX, y - dragStartY);
    }

    /**
     * Moves elements together by one offset from where each started, cut short on each axis by
     * the member closest to that edge of the screen, so none leaves it and they keep their
     * arrangement. Members not drawn in the last frame stay where they are.
     */
    private void move(List<Member> members, double dx, double dy) {
        double minDx = Double.NEGATIVE_INFINITY;
        double maxDx = Double.POSITIVE_INFINITY;
        double minDy = Double.NEGATIVE_INFINITY;
        double maxDy = Double.POSITIVE_INFINITY;
        List<Member> moving = new ArrayList<>();
        List<Placed> current = new ArrayList<>();
        for (Member member : members) {
            Placed now = find(member.registered);
            if (now == null) continue;
            // The element's size can change while it moves (a clock's text), so the limit uses its latest one.
            minDx = Math.max(minDx, -member.x);
            maxDx = Math.min(maxDx, lastWidth - now.bounds.width - member.x);
            minDy = Math.max(minDy, -member.y);
            maxDy = Math.min(maxDy, lastHeight - now.bounds.height - member.y);
            moving.add(member);
            current.add(now);
        }
        if (moving.isEmpty()) return;
        if (maxDx < minDx) maxDx = minDx;
        if (maxDy < minDy) maxDy = minDy;
        double offsetX = Math.max(minDx, Math.min(dx, maxDx));
        double offsetY = Math.max(minDy, Math.min(dy, maxDy));
        for (Member member : moving) {
            member.registered.getPlacement().moveTo((member.x + offsetX) / lastWidth, (member.y + offsetY) / lastHeight);
        }
    }

    private void endDrag() {
        dragging = null;
        dragGroup = null;
        dragRecorded = false;
        dragSeenDown = false;
        HudRegistry.save();
    }

    @Override
    protected boolean mouseUp(double x, double y, int button) {
        if (popup != null) {
            popup.mouseUp(x, y, button);
            checkPopup();
            return true;
        }
        if (dragging == null) return false;
        endDrag();
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        if (popup != null) {
            popup.scroll(x, y, amount);
            checkPopup();
            return true;
        }
        Hit hit = hitAt(x, y);
        if (hit == null || amount == 0) return false;
        if (hit.group != null) {
            scaleGroup(hit.group, Math.signum(amount));
            return true;
        }
        HudPlacement placement = hit.element.registered.getPlacement();
        double scale = Math.round((placement.getScale() + Math.signum(amount) * SCALE_STEP) * 20.0) / 20.0;
        HudHistory.record();
        placement.scaleTo(scale);
        HudRegistry.save();
        return true;
    }

    /**
     * Scales a group's drawn members by one step, all by the same ratio, and scales each member's
     * offset from the group's top-left corner by it too, so the group keeps its shape. The step is
     * taken from the smallest member and cut short so no member passes the scale limits; the
     * scaled group is then shifted as a whole to stay on screen.
     *
     * @param direction {@code 1} to grow, {@code -1} to shrink
     */
    private void scaleGroup(String groupId, double direction) {
        List<Placed> members = placedIn(groupId);
        if (members.isEmpty()) return;
        double smallest = Double.POSITIVE_INFINITY;
        double largest = 0;
        int originX = Integer.MAX_VALUE;
        int originY = Integer.MAX_VALUE;
        for (Placed member : members) {
            double scale = member.registered.getPlacement().getScale();
            smallest = Math.min(smallest, scale);
            largest = Math.max(largest, scale);
            originX = Math.min(originX, member.bounds.x);
            originY = Math.min(originY, member.bounds.y);
        }
        double target = Math.round((smallest + direction * SCALE_STEP) * 20.0) / 20.0;
        double ratio = target / smallest;
        ratio = Math.max(HudPlacement.MIN_SCALE / smallest, Math.min(ratio, HudPlacement.MAX_SCALE / largest));
        if (Math.abs(ratio - 1) < 1e-9) return;

        double[][] rects = new double[members.size()][];
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < members.size(); i++) {
            HudOverlay.Bounds b = members.get(i).bounds;
            double x = originX + (b.x - originX) * ratio;
            double y = originY + (b.y - originY) * ratio;
            double w = b.width * ratio;
            double h = b.height * ratio;
            rects[i] = new double[] {x, y, w, h};
            left = Math.min(left, x);
            top = Math.min(top, y);
            right = Math.max(right, x + w);
            bottom = Math.max(bottom, y + h);
        }
        double shiftX = 0;
        double shiftY = 0;
        if (right > lastWidth) shiftX = lastWidth - right;
        if (left + shiftX < 0) shiftX = -left;
        if (bottom > lastHeight) shiftY = lastHeight - bottom;
        if (top + shiftY < 0) shiftY = -top;

        HudHistory.record();
        for (int i = 0; i < members.size(); i++) {
            HudPlacement placement = members.get(i).registered.getPlacement();
            double[] r = rects[i];
            placement.scaleTo(Math.round(placement.getScale() * ratio * 1000.0) / 1000.0);
            double x = Math.max(0, Math.min(r[0] + shiftX, lastWidth - r[2]));
            double y = Math.max(0, Math.min(r[1] + shiftY, lastHeight - r[3]));
            placement.moveTo(x / lastWidth, y / lastHeight);
        }
        HudRegistry.save();
    }

    // ---------------------------------------------------------------- keys

    @Override
    protected boolean key(int key, int modifiers) {
        if (popup != null) {
            // The popup gets every key, Escape included, so Escape closes it rather than the editor.
            popup.key(key, modifiers);
            checkPopup();
            return true;
        }
        boolean shortcut = (modifiers & Theme.GLFW_MOD_SHORTCUT) != 0;
        if (shortcut && (key == Theme.GLFW_Z || key == Theme.GLFW_Y)) {
            if (dragging != null) return true;
            boolean redo = key == Theme.GLFW_Y || (modifiers & Theme.GLFW_MOD_SHIFT) != 0;
            boolean changed = redo ? HudHistory.redo() : HudHistory.undo();
            if (changed) pruneSelectionToLayout();
            return true;
        }
        if (dragging != null) return false;

        int step = shortcut ? NUDGE_FINE : NUDGE;
        switch (key) {
            case Theme.GLFW_LEFT: return nudge(-step, 0);
            case Theme.GLFW_RIGHT: return nudge(step, 0);
            case Theme.GLFW_UP: return nudge(0, -step);
            case Theme.GLFW_DOWN: return nudge(0, step);
            case Theme.GLFW_DELETE: return removeSelection();
            case Theme.GLFW_G: return groupSelection();
            case Theme.GLFW_U: return ungroupSelection();
            case Theme.GLFW_E: return editTarget();
            case Theme.GLFW_R: return resetHovered();
            default: return false;
        }
    }

    /** Moves the selection by whole GUI pixels, as one, as far as its most blocked element can go. */
    private boolean nudge(int dx, int dy) {
        List<Placed> selection = selection();
        if (selection.isEmpty()) return false;
        List<Member> members = new ArrayList<>();
        for (Placed entry : selection) members.add(new Member(entry));
        HudHistory.record();
        move(members, dx, dy);
        HudRegistry.save();
        return true;
    }

    /** Removes the selected elements, or every member of the selected group. */
    private boolean removeSelection() {
        if (selectedGroup == null && selectedKeys.isEmpty()) return false;
        HudHistory.record();
        if (selectedGroup != null) {
            for (HudRegistry.Registered member : HudRegistry.members(selectedGroup)) member.getPlacement().remove();
        } else {
            for (Placed entry : selection()) entry.registered.getPlacement().remove();
        }
        clearSelection();
        HudRegistry.save();
        return true;
    }

    /** Ties the selected elements into a new group, which becomes the selection. */
    private boolean groupSelection() {
        if (selectedKeys.size() < 2) return false;
        HudHistory.record();
        HudGroup group = HudRegistry.createGroup(new ArrayList<>(selectedKeys));
        if (group == null) return true;
        clearSelection();
        selectedGroup = group.getId();
        HudRegistry.save();
        return true;
    }

    /**
     * Dissolves the selected group, the groups of the selected elements, or with nothing selected
     * the group under the pointer. Its former members become the selection.
     */
    private boolean ungroupSelection() {
        Set<String> groups = new LinkedHashSet<>();
        if (selectedGroup != null) {
            groups.add(selectedGroup);
        } else if (! selectedKeys.isEmpty()) {
            for (Placed entry : selection()) {
                String group = entry.registered.getPlacement().getGroup();
                if (group != null) groups.add(group);
            }
        } else {
            Hit hit = hitAt(lastMouseX, lastMouseY);
            if (hit != null && hit.group != null) groups.add(hit.group);
        }
        if (groups.isEmpty()) return false;
        HudHistory.record();
        clearSelection();
        for (String group : groups) {
            for (Placed member : placedIn(group)) selectedKeys.add(member.registered.getKey());
            HudRegistry.ungroup(group);
        }
        HudRegistry.save();
        return true;
    }

    /** Opens the frame popup for what is under the pointer, or else for the selection. */
    private boolean editTarget() {
        Hit hit = hitAt(lastMouseX, lastMouseY);
        if (hit == null && selectedGroup != null) {
            HudOverlay.Bounds area = groupAreas.get(selectedGroup);
            if (area != null) hit = new Hit(null, selectedGroup, area);
        }
        if (hit == null && selectedKeys.size() == 1) {
            Placed entry = find(selectedKeys.iterator().next());
            if (entry != null) hit = new Hit(entry, null, entry.bounds);
        }
        if (hit == null) return false;
        openPopup(hit);
        swallowE = popup != null;
        return true;
    }

    /** Puts the element or every group member under the pointer back at its default place. */
    private boolean resetHovered() {
        Hit hit = hitAt(lastMouseX, lastMouseY);
        if (hit == null) return false;
        HudHistory.record();
        if (hit.group != null) {
            for (HudRegistry.Registered member : editableMembers(hit.group)) {
                HudRegistry.reset(member.getOwner(), member.getElement());
            }
        } else {
            HudRegistry.reset(hit.element.registered.getOwner(), hit.element.registered.getElement());
        }
        HudRegistry.save();
        return true;
    }

    @Override
    protected boolean typed(String text) {
        if (popup == null) return false;
        if (swallowE && text.equalsIgnoreCase("e")) {
            swallowE = false;
            return true;
        }
        for (int i = 0; i < text.length(); i++) popup.charTyped(text.charAt(i));
        checkPopup();
        return true;
    }

    @Override
    public void onClose() {
        if (popup != null) {
            closePopup();
            return;
        }
        HudRegistry.save();
        ClientCompat.setScreen(parent);
    }
}
