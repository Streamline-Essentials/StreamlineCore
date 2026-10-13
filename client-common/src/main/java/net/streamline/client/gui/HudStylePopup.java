package net.streamline.client.gui;

import net.streamline.client.Canvas;
import net.streamline.client.CanvasHudRenderer;
import net.streamline.client.HudOverlay;
import net.streamline.client.compat.ClientCompat;
import singularity.client.hud.HudBoxStyle;
import singularity.client.hud.HudElement;
import singularity.client.hud.HudGroup;
import singularity.client.hud.HudHistory;
import singularity.client.hud.HudRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The frame and group editor of one HUD element or one {@link HudGroup}: a Frame section with
 * the background and border switches, their colors (swatch, {@code #AARRGGBB} field, presets and
 * an opacity slider) and a live preview, then a Group section to leave the element's group, or
 * to rename a group, unlink its members and ungroup it.
 *
 * <p>{@link #draw} shows it as a modal popup over the whole screen, used by the HUD editor and
 * the menu's group list. The module menu embeds the same sections in its own element popup
 * through {@link #drawBody} and {@link #clickBody}.</p>
 *
 * <p>The element or group is looked up again every frame by its key or id, since undo, redo and
 * resets replace placements and groups; the popup closes once it is gone. Every change records a
 * {@link HudHistory} step first and saves the layout after. Render thread only.</p>
 */
final class HudStylePopup {

    private static final int PAD = 8;
    private static final int SCROLL_STEP = 16;
    private static final int SWATCH = 10;
    private static final int PREVIEW_HEIGHT = 36;
    private static final int PREVIEW_MAX_HEIGHT = 100;
    private static final int NAME_MAX_LENGTH = 32;
    private static final int HEX_MAX_LENGTH = 9;
    /** Behind the preview, so translucent backgrounds read as they do over the world. */
    private static final int PREVIEW_SKY = 0xFF86A8D8;
    private static final int PREVIEW_GROUND = 0xFF5E8C3E;
    /** Preset colors, RGB only: picking one keeps the part's opacity. */
    private static final int[] PRESETS = {
            0x000000, 0xFFFFFF, 0x808080, Theme.ACCENT & 0xFFFFFF, 0xFF9800, 0xFFEB3B, 0x4CC26A, Theme.TAG & 0xFFFFFF, 0x9C27B0
    };

    /** A part of the frame: each has its own switch and color. */
    private enum Part {
        BACKGROUND("Background"),
        BORDER("Border");

        final String label;

        Part(String label) {
            this.label = label;
        }

        boolean on(HudBoxStyle style) {
            return this == BACKGROUND ? style.isBackground() : style.isBorder();
        }

        void setOn(HudBoxStyle style, boolean on) {
            if (this == BACKGROUND) style.setBackground(on);
            else style.setBorder(on);
        }

        int color(HudBoxStyle style) {
            return this == BACKGROUND ? style.getBackgroundColor() : style.getBorderColor();
        }

        void setColor(HudBoxStyle style, int color) {
            if (this == BACKGROUND) style.setBackgroundColor(color);
            else style.setBorderColor(color);
        }
    }

    private interface Click {
        void click(double x, double y, int button);
    }

    private static final class Region {
        final int x1, y1, x2, y2;
        final Object owner;
        final Click click;

        Region(int x1, int y1, int x2, int y2, Object owner, Click click) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.owner = owner;
            this.click = click;
        }
    }

    /** The registry key of the element edited, or {@code null} for a group. */
    private final String elementKey;
    /** The id of the group edited, or {@code null} for an element. */
    private final String groupId;
    private boolean closed;
    /** Told whether a field has focus; 26.x sends typed characters only while one has. */
    private Consumer<Boolean> textInput;
    /** Whether {@link #drawBody} draws the preview; off when the caller draws it elsewhere. */
    private boolean bodyPreview = true;

    /** Clickable areas of the sections, recorded by {@link #drawBody}. */
    private final List<Region> bodyRegions = new ArrayList<>();
    /** The standalone popup's own areas: the dimmed screen, the panel and Close. */
    private final List<Region> frameRegions = new ArrayList<>();
    /** The vertical span regions and hovers are limited to while drawing the body. */
    private int clipY1, clipY2;

    private double scroll;
    private int overflow;
    /** The body's height in the last frame, which sizes the standalone panel. */
    private int bodyHeight = 170;

    /** The focused field: a {@link Part} for its color, or {@link #NAME_FIELD}; {@code null} for none. */
    private Object fieldOwner;
    private final StringBuilder fieldText = new StringBuilder();
    private int fieldMax;
    /** Whether the whole text is selected, so the next character typed replaces it. */
    private boolean fieldFresh;
    private long focusedAt;
    private static final Object NAME_FIELD = new Object();

    /** The part whose opacity slider is held, and its track as last drawn. */
    private Part dragging;
    private int dragX1, dragX2;

    private HudStylePopup(String elementKey, String groupId) {
        this.elementKey = elementKey;
        this.groupId = groupId;
    }

    static HudStylePopup forElement(HudRegistry.Registered registered) {
        return new HudStylePopup(registered.getKey(), null);
    }

    static HudStylePopup forGroup(HudGroup group) {
        return new HudStylePopup(null, group.getId());
    }

    /**
     * Sets what is told when a field of the popup gains or loses focus; a screen passes its
     * {@code textInput}, without which 26.x delivers no typed characters.
     */
    HudStylePopup onTextInput(Consumer<Boolean> listener) {
        this.textInput = listener;
        return this;
    }

    /** Whether {@link #drawBody} includes the preview; the menu draws it under the text field instead. */
    void setBodyPreview(boolean bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    boolean isClosed() {
        return closed;
    }

    /** Closes the popup, applying what was typed into its field. */
    void close() {
        commitField();
        endDrag();
        closed = true;
    }

    // ---------------------------------------------------------------- targets

    private HudRegistry.Registered registered() {
        if (elementKey == null) return null;
        for (HudRegistry.Registered registered : HudRegistry.elements()) {
            if (registered.getKey().equals(elementKey)) return registered;
        }
        return null;
    }

    private HudGroup group() {
        return HudRegistry.group(groupId);
    }

    /** @return the frame being edited, or {@code null} once the element or group is gone */
    private HudBoxStyle style() {
        if (elementKey != null) {
            HudRegistry.Registered registered = registered();
            return registered == null ? null : registered.getPlacement().getStyle();
        }
        HudGroup group = group();
        return group == null ? null : group.getStyle();
    }

    private String title() {
        if (elementKey != null) {
            HudRegistry.Registered registered = registered();
            return registered == null ? elementKey : displayName(registered);
        }
        HudGroup group = group();
        return group == null ? "" : group.getName() + " &r&8(group)";
    }

    /** Element methods are module code, so a failing one falls back to the key. */
    private static String displayName(HudRegistry.Registered registered) {
        try {
            return registered.getElement().getDisplayName();
        } catch (Throwable t) {
            return registered.getKey();
        }
    }

    /** Records an undo step, makes the change, and saves the layout. */
    private static void change(Runnable mutation) {
        HudHistory.record();
        mutation.run();
        HudRegistry.save();
    }

    // ---------------------------------------------------------------- standalone popup

    /** Draws the popup centred over the whole screen, dimming everything behind it. */
    void draw(Canvas canvas, int mouseX, int mouseY) {
        frameRegions.clear();
        if (closed) return;
        if (style() == null) {
            close();
            return;
        }
        frameRegions.add(new Region(0, 0, canvas.width(), canvas.height(), null, (x, y, b) -> close()));
        canvas.fill(0, 0, canvas.width(), canvas.height(), 0xA0000000);

        int line = canvas.lineHeight() + 2;
        int width = Math.min(canvas.width() - 40, 300);
        int height = Math.min(PAD + line + 4 + bodyHeight + 8 + 14 + PAD, canvas.height() - 20);
        int x1 = (canvas.width() - width) / 2;
        int y1 = (canvas.height() - height) / 2;
        int x2 = x1 + width;
        int y2 = y1 + height;
        frameRegions.add(new Region(x1, y1, x2, y2, null, (x, y, b) -> { }));
        canvas.fill(x1, y1, x2, y2, Theme.PANEL);
        canvas.outline(x1, y1, x2, y2, Theme.CONTROL_BORDER);
        canvas.text(Theme.fit(canvas, "&l" + title(), width - PAD * 2), x1 + PAD, y1 + PAD, Theme.TEXT, false);

        int viewY1 = y1 + PAD + line + 4;
        int viewY2 = y2 - PAD - 18;
        overflow = Math.max(0, bodyHeight - (viewY2 - viewY1));
        scroll = Math.max(0, Math.min(scroll, overflow));
        canvas.clip(x1, viewY1, x2, viewY2);
        int start = viewY1 - (int) scroll;
        int end = drawBody(canvas, mouseX, mouseY, x1 + PAD, start, x2 - PAD - 4, viewY1, viewY2);
        canvas.endClip();
        bodyHeight = end - start;
        if (overflow > 0) {
            int track = viewY2 - viewY1;
            int barHeight = Math.max(12, track * track / (track + overflow));
            int barY = viewY1 + (int) ((track - barHeight) * (scroll / overflow));
            canvas.fill(x2 - 3, barY, x2 - 1, barY + barHeight, 0x60FFFFFF);
        }

        int closeX2 = x2 - PAD;
        int closeX1 = closeX2 - 50;
        int closeY1 = y2 - PAD - 14;
        Theme.button(canvas, closeX1, closeY1, closeX2, closeY1 + 14, "Close",
                Theme.inside(mouseX, mouseY, closeX1, closeY1, closeX2, closeY1 + 14), true);
        frameRegions.add(new Region(closeX1, closeY1, closeX2, closeY1 + 14, null, (x, y, b) -> close()));
    }

    /** Takes every click while open; a click outside the panel closes it. */
    boolean mouseDown(double x, double y, int button) {
        if (closed) return false;
        if (clickBody(x, y, button)) return true;
        for (int i = frameRegions.size() - 1; i >= 0; i--) {
            Region region = frameRegions.get(i);
            if (! Theme.inside(x, y, region.x1, region.y1, region.x2, region.y2)) continue;
            region.click.click(x, y, button);
            return true;
        }
        return true;
    }

    /** Lets go of a held opacity slider, saving where it ended. */
    boolean mouseUp(double x, double y, int button) {
        if (button == 0) endDrag();
        return ! closed;
    }

    boolean scroll(double x, double y, double amount) {
        if (closed) return false;
        scroll = Math.max(0, Math.min(overflow, scroll - amount * SCROLL_STEP));
        return true;
    }

    /**
     * Types into the focused field; with none, Escape closes the popup. Every key is taken while
     * the popup is open, so nothing behind it reacts.
     */
    boolean key(int key, int modifiers) {
        if (closed) return false;
        if (fieldKey(key, modifiers)) return true;
        if (key == Theme.GLFW_ESCAPE) close();
        return true;
    }

    boolean charTyped(char c) {
        if (closed) return false;
        if (fieldOwner == null) return true;
        if (c < 32 || c == 127) return true;
        if (fieldOwner instanceof Part && ! (c == '#' || Character.digit(c, 16) >= 0)) return true;
        if (fieldFresh) {
            fieldText.setLength(0);
            fieldFresh = false;
        }
        if (fieldText.length() < fieldMax) fieldText.append(fieldOwner instanceof Part ? Character.toUpperCase(c) : c);
        return true;
    }

    // ---------------------------------------------------------------- body

    /**
     * Draws the Frame and Group sections from {@code y} down, between {@code x1} and {@code x2},
     * recording their clickable areas for {@link #clickBody}; only what lies between
     * {@code clipY1} and {@code clipY2} can be hovered or clicked.
     *
     * @return the y below the sections
     */
    int drawBody(Canvas canvas, int mouseX, int mouseY, int x1, int y, int x2, int clipY1, int clipY2) {
        bodyRegions.clear();
        this.clipY1 = clipY1;
        this.clipY2 = clipY2;
        HudBoxStyle style = style();
        if (style == null) {
            close();
            return y;
        }
        if (dragging != null && ! ClientCompat.leftMouseDown()) endDrag();

        y = section(canvas, "Frame", x1, y, x2);
        y = drawPart(canvas, mouseX, mouseY, Part.BACKGROUND, style, x1, y, x2);
        y = drawPart(canvas, mouseX, mouseY, Part.BORDER, style, x1, y, x2);
        if (bodyPreview) y = drawPreview(canvas, x1, y + 2, x2) + 4;

        y += 4;
        y = section(canvas, "Group", x1, y, x2);
        if (elementKey != null) y = drawElementGroup(canvas, mouseX, mouseY, x1, y, x2);
        else y = drawGroupMembers(canvas, mouseX, mouseY, x1, y, x2);
        return y;
    }

    /**
     * Handles a click on the sections as last drawn by {@link #drawBody}. A click anywhere but
     * the focused field applies what was typed into it.
     *
     * @return whether one of the sections' controls took the click
     */
    boolean clickBody(double x, double y, int button) {
        if (closed) return false;
        for (int i = bodyRegions.size() - 1; i >= 0; i--) {
            Region region = bodyRegions.get(i);
            if (! Theme.inside(x, y, region.x1, region.y1, region.x2, region.y2)) continue;
            if (fieldOwner != null && region.owner != fieldOwner) commitField();
            region.click.click(x, y, button);
            return true;
        }
        commitField();
        return false;
    }

    private static int section(Canvas canvas, String title, int x1, int y, int x2) {
        canvas.text(title.toUpperCase(Locale.ROOT), x1, y, Theme.ACCENT);
        canvas.fill(x1, y + canvas.lineHeight() + 1, x2, y + canvas.lineHeight() + 2, Theme.CONTROL_BORDER);
        return y + canvas.lineHeight() + 6;
    }

    private boolean hover(int mouseX, int mouseY, int x1, int y1, int x2, int y2) {
        return y2 > clipY1 && y1 < clipY2 && Theme.inside(mouseX, mouseY, x1, Math.max(y1, clipY1), x2, Math.min(y2, clipY2));
    }

    private void addRegion(int x1, int y1, int x2, int y2, Object owner, Click click) {
        if (y2 <= clipY1 || y1 >= clipY2) return;
        bodyRegions.add(new Region(x1, Math.max(y1, clipY1), x2, Math.min(y2, clipY2), owner, click));
    }

    /**
     * One part of the frame on two rows: its switch, name, swatch and hex field; then the
     * presets and the opacity slider.
     */
    private int drawPart(Canvas canvas, int mouseX, int mouseY, Part part, HudBoxStyle style, int x1, int y, int x2) {
        boolean on = part.on(style);
        int color = part.color(style);

        Theme.toggle(canvas, x1, y, on, hover(mouseX, mouseY, x1, y, x1 + 20, y + 10));
        // Sets the opposite of what this frame shows, so a color applied by the same click (which
        // turns the part on) is not switched straight back off.
        Click toggle = (cx, cy, b) -> change(() -> {
            HudBoxStyle current = style();
            if (current != null) part.setOn(current, ! on);
        });
        addRegion(x1, y, x1 + 20, y + 10, null, toggle);

        int fieldX1 = x2 - Math.max(58, canvas.rawTextWidth("#DDDDDDDD") + 8);
        int swatchX1 = fieldX1 - 4 - (SWATCH + 2);
        int labelX = x1 + 26;
        canvas.text(Theme.fit(canvas, part.label, swatchX1 - 6 - labelX), labelX, y + 1, on ? Theme.TEXT : Theme.MUTED);
        addRegion(labelX - 2, y - 1, swatchX1 - 4, y + 11, null, toggle);

        canvas.fill(swatchX1, y - 1, swatchX1 + SWATCH + 2, y + SWATCH + 1, Theme.CONTROL_BORDER);
        swatch(canvas, swatchX1 + 1, y, color);
        field(canvas, mouseX, mouseY, part, fieldX1, y, x2, hex(color), HEX_MAX_LENGTH);
        addRegion(swatchX1, y - 1, swatchX1 + SWATCH + 2, y + SWATCH + 1, part, (cx, cy, b) -> focus(part, hex(color), HEX_MAX_LENGTH));
        y += 15;

        int x = labelX;
        for (int preset : PRESETS) {
            int px1 = x;
            int px2 = x + SWATCH;
            boolean hovered = hover(mouseX, mouseY, px1 - 1, y - 1, px2 + 1, y + SWATCH + 1);
            boolean current = (color & 0xFFFFFF) == preset;
            canvas.fill(px1 - 1, y - 1, px2 + 1, y + SWATCH + 1, hovered ? 0xFFFFFFFF : (current ? Theme.ACCENT_LIGHT : Theme.CONTROL_BORDER));
            canvas.fill(px1, y, px2, y + SWATCH, 0xFF000000 | preset);
            addRegion(px1 - 1, y - 1, px2 + 1, y + SWATCH + 1, null, (cx, cy, b) -> change(() -> {
                HudBoxStyle target = style();
                if (target == null) return;
                int alpha = part.color(target) >>> 24;
                part.setColor(target, ((alpha == 0 ? 0xFF : alpha) << 24) | preset);
                part.setOn(target, true);
            }));
            x = px2 + 4;
        }

        int percentWidth = canvas.textWidth("100%");
        int trackX1 = x + 6;
        int trackX2 = x2 - percentWidth - 6;
        if (trackX2 - trackX1 >= 24) {
            if (dragging == part) {
                dragX1 = trackX1;
                dragX2 = trackX2;
                setAlpha(part, mouseX);
            }
            int alpha = part.color(style) >>> 24;
            int shownAlpha = alpha == 0 ? 0xFF : alpha;
            String percent = Math.round(shownAlpha * 100 / 255.0) + "%";
            canvas.text(percent, x2 - canvas.textWidth(percent), y + 1, Theme.MUTED);
            canvas.fill(trackX1, y + 4, trackX2, y + 6, Theme.SWITCH_OFF);
            int knobX = trackX1 + (int) Math.round((shownAlpha - 1) / 254.0 * (trackX2 - trackX1 - 4));
            canvas.fill(trackX1, y + 4, knobX + 2, y + 6, Theme.ACCENT);
            boolean hovered = dragging == part || hover(mouseX, mouseY, trackX1, y, trackX2, y + 10);
            canvas.fill(knobX, y + 1, knobX + 4, y + 9, hovered ? 0xFFFFFFFF : 0xFFD0D0D0);
            addRegion(trackX1 - 2, y - 1, trackX2 + 2, y + 11, null, (cx, cy, b) -> {
                if (b != 0) return;
                HudHistory.record();
                dragging = part;
                dragX1 = trackX1;
                dragX2 = trackX2;
                setAlpha(part, cx);
            });
        }
        return y + 16;
    }

    /** Sets the opacity of the held slider's part from the pointer's x; it never reaches 0, which draws opaque. */
    private void setAlpha(Part part, double mouseX) {
        HudBoxStyle style = style();
        if (style == null) return;
        double fraction = Math.max(0, Math.min(1, (mouseX - dragX1) / (double) Math.max(1, dragX2 - dragX1)));
        int alpha = 1 + (int) Math.round(fraction * 254);
        part.setColor(style, (alpha << 24) | (part.color(style) & 0xFFFFFF));
    }

    private void endDrag() {
        if (dragging == null) return;
        dragging = null;
        HudRegistry.save();
    }

    /** A color swatch over a checkerboard, so its opacity shows; zero alpha draws opaque, as on the HUD. */
    private static void swatch(Canvas canvas, int x1, int y1, int color) {
        int half = SWATCH / 2;
        canvas.fill(x1, y1, x1 + SWATCH, y1 + SWATCH, 0xFFC8C8C8);
        canvas.fill(x1, y1, x1 + half, y1 + half, 0xFF7A7A7A);
        canvas.fill(x1 + half, y1 + half, x1 + SWATCH, y1 + SWATCH, 0xFF7A7A7A);
        canvas.fill(x1, y1, x1 + SWATCH, y1 + SWATCH, Canvas.opaque(color));
    }

    private static String hex(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    /**
     * The element, or for a group its name and member count, drawn with the frame over a strip
     * of sky and ground, scaled down to fit.
     *
     * @return the y below the preview
     */
    int drawPreview(Canvas canvas, int x1, int y, int x2) {
        HudBoxStyle style = style();
        if (style == null) return y;
        int roomWidth = x2 - x1 - 8;

        if (elementKey != null) {
            HudRegistry.Registered registered = registered();
            if (registered == null) return y;
            HudElement element = registered.getElement();
            int y2 = y + PREVIEW_HEIGHT;
            try {
                CanvasHudRenderer renderer = new CanvasHudRenderer(canvas, style.isBackground());
                int width = element.width(renderer, true);
                int height = element.height(renderer, true);
                if (width <= 0 || height <= 0) {
                    previewBackdrop(canvas, x1, y, x2, y2);
                    centredText(canvas, "&7Nothing to show right now", x1, y, x2, y2);
                    return y2;
                }
                // The strip grows with the element up to PREVIEW_MAX_HEIGHT; past that, and past
                // the width, the element is scaled down.
                int stripHeight = Math.max(PREVIEW_HEIGHT, Math.min(height + 8, PREVIEW_MAX_HEIGHT));
                y2 = y + stripHeight;
                previewBackdrop(canvas, x1, y, x2, y2);
                float scale = Math.min(1f, Math.min(roomWidth / (float) width, (stripHeight - 8) / (float) height));
                int shownWidth = (int) Math.ceil(width * scale);
                int shownHeight = (int) Math.ceil(height * scale);
                HudOverlay.Bounds bounds = new HudOverlay.Bounds(x1 + (x2 - x1 - shownWidth) / 2,
                        y + (stripHeight - shownHeight) / 2, shownWidth, shownHeight);
                HudOverlay.drawFrame(canvas, style, bounds, true, false);
                canvas.push();
                try {
                    canvas.translate(bounds.x, bounds.y);
                    canvas.scale(scale);
                    element.render(renderer, true);
                } finally {
                    canvas.pop();
                }
                HudOverlay.drawFrame(canvas, style, bounds, false, true);
            } catch (Throwable t) {
                centredText(canvas, "&cPreview failed", x1, y, x2, y2);
            }
            return y2;
        }

        HudGroup group = group();
        if (group == null) return y;
        int y2 = y + PREVIEW_HEIGHT;
        previewBackdrop(canvas, x1, y, x2, y2);
        int count = HudRegistry.memberKeys(groupId).size();
        String name = Theme.fit(canvas, group.getName(), roomWidth - 6);
        String members = count + (count == 1 ? " element" : " elements");
        int line = canvas.lineHeight() + 1;
        int width = Math.max(canvas.textWidth(name), canvas.textWidth(members)) + 6;
        int height = line * 2 + 4;
        HudOverlay.Bounds bounds = new HudOverlay.Bounds(x1 + (x2 - x1 - width) / 2, y + (PREVIEW_HEIGHT - height) / 2, width, height);
        HudOverlay.drawFrame(canvas, style, bounds, true, false);
        canvas.text(name, bounds.x + 3, bounds.y + 3, 0xFFFFFFFF, true);
        canvas.text(members, bounds.x + 3, bounds.y + 3 + line, 0xFFD0D0D0, true);
        HudOverlay.drawFrame(canvas, style, bounds, false, true);
        return y2;
    }

    private static void previewBackdrop(Canvas canvas, int x1, int y1, int x2, int y2) {
        canvas.fill(x1, y1, x2, y2, PREVIEW_SKY);
        canvas.fill(x1, y2 - 9, x2, y2, PREVIEW_GROUND);
        canvas.outline(x1, y1, x2, y2, Theme.CONTROL_BORDER);
    }

    private static void centredText(Canvas canvas, String text, int x1, int y1, int x2, int y2) {
        canvas.text(text, x1 + (x2 - x1 - canvas.textWidth(text)) / 2, y1 + (y2 - y1 - canvas.lineHeight()) / 2 + 1, Theme.TEXT, true);
    }

    /** The element's group, with Unlink, or how to make one. */
    private int drawElementGroup(Canvas canvas, int mouseX, int mouseY, int x1, int y, int x2) {
        HudRegistry.Registered registered = registered();
        HudGroup group = registered == null ? null : HudRegistry.groupOf(registered);
        if (group == null) {
            for (String line : Theme.wrap(canvas, "Not in a group. Select elements with Ctrl+click in the HUD Editor and press G to group them.", x2 - x1)) {
                canvas.text(line, x1, y, Theme.MUTED);
                y += canvas.lineHeight() + 2;
            }
            return y;
        }
        int unlinkX1 = x2 - canvas.textWidth("Unlink") - 14;
        canvas.text(Theme.fit(canvas, "&7In &f" + group.getName(), unlinkX1 - 6 - x1), x1, y + 3, Theme.TEXT);
        button(canvas, mouseX, mouseY, unlinkX1, y, x2, y + 14, "Unlink", false,
                () -> change(() -> HudRegistry.unlink(elementKey)));
        return y + 18;
    }

    /** A group's name field, its members each with Unlink, and Ungroup. */
    private int drawGroupMembers(Canvas canvas, int mouseX, int mouseY, int x1, int y, int x2) {
        HudGroup group = group();
        if (group == null) return y;
        int labelWidth = canvas.textWidth("Name") + 8;
        canvas.text("Name", x1, y + 1, Theme.TEXT);
        field(canvas, mouseX, mouseY, NAME_FIELD, x1 + labelWidth, y, x2, group.getName(), NAME_MAX_LENGTH);
        y += 18;

        List<HudRegistry.Registered> registered = HudRegistry.members(groupId);
        List<String> keys = HudRegistry.memberKeys(groupId);
        Collections.sort(keys);
        canvas.text("Members", x1, y, Theme.MUTED);
        y += canvas.lineHeight() + 4;
        for (String key : keys) {
            String name = key;
            for (HudRegistry.Registered member : registered) {
                if (member.getKey().equals(key)) {
                    name = displayName(member);
                    break;
                }
            }
            if (hover(mouseX, mouseY, x1 - 2, y - 2, x2 + 2, y + 13)) canvas.fill(x1 - 2, y - 2, x2 + 2, y + 13, Theme.HOVER);
            int unlinkX1 = x2 - canvas.textWidth("Unlink") - 10;
            boolean running = registeredKey(registered, key);
            canvas.text(Theme.fit(canvas, name + (running ? "" : " &8(not running)"), unlinkX1 - 6 - x1), x1, y + 1, Theme.TEXT);
            String memberKey = key;
            button(canvas, mouseX, mouseY, unlinkX1, y - 1, x2, y + 11, "Unlink", false,
                    () -> change(() -> HudRegistry.unlink(memberKey)));
            y += 15;
        }
        y += 3;
        int ungroupX2 = x1 + canvas.textWidth("Ungroup") + 14;
        button(canvas, mouseX, mouseY, x1, y, ungroupX2, y + 14, "Ungroup", true, () -> {
            change(() -> HudRegistry.ungroup(groupId));
            close();
        });
        return y + 18;
    }

    private static boolean registeredKey(List<HudRegistry.Registered> registered, String key) {
        for (HudRegistry.Registered member : registered) {
            if (member.getKey().equals(key)) return true;
        }
        return false;
    }

    private void button(Canvas canvas, int mouseX, int mouseY, int x1, int y1, int x2, int y2, String label, boolean primary, Runnable action) {
        Theme.button(canvas, x1, y1, x2, y2, label, hover(mouseX, mouseY, x1, y1, x2, y2), primary);
        addRegion(x1, y1, x2, y2, null, (cx, cy, b) -> action.run());
    }

    // ---------------------------------------------------------------- field

    /**
     * A one-line field: clicking it selects its whole text, so typing replaces it; clicking it
     * again keeps the text and types after it. Enter, Escape or a click elsewhere applies it.
     */
    private void field(Canvas canvas, int mouseX, int mouseY, Object owner, int x1, int y, int x2, String value, int maxLength) {
        boolean focused = fieldOwner == owner;
        boolean hovered = hover(mouseX, mouseY, x1, y - 1, x2, y + 11);
        canvas.fill(x1, y - 1, x2, y + 11, Theme.CONTROL);
        canvas.outline(x1, y - 1, x2, y + 11, focused ? Theme.ACCENT : (hovered ? Theme.MUTED : Theme.CONTROL_BORDER));
        int room = x2 - x1 - 7;
        String shown;
        if (focused) {
            // The end stays in view while typing.
            shown = fieldText.toString();
            while (! shown.isEmpty() && canvas.rawTextWidth(shown) > room) shown = shown.substring(1);
        } else {
            shown = value == null ? "" : value;
            while (! shown.isEmpty() && canvas.rawTextWidth(shown) > room) shown = shown.substring(0, shown.length() - 1);
        }
        if (focused && fieldFresh && ! shown.isEmpty()) {
            canvas.fill(x1 + 3, y, x1 + 3 + canvas.rawTextWidth(shown), y + 10, Theme.SELECTION);
        }
        canvas.rawText(shown, x1 + 3, y + 1, Theme.TEXT);
        if (focused && ! fieldFresh && ((System.currentTimeMillis() - focusedAt) / 500) % 2 == 0) {
            int caretX = x1 + 3 + canvas.rawTextWidth(shown);
            canvas.fill(caretX, y, caretX + 1, y + 10, Theme.TEXT);
        }
        addRegion(x1, y - 1, x2, y + 11, owner, (cx, cy, b) -> focus(owner, value, maxLength));
    }

    /** @return whether a field of the popup has focus */
    boolean hasFocus() {
        return fieldOwner != null;
    }

    private void focus(Object owner, String value, int maxLength) {
        if (fieldOwner == owner) {
            fieldFresh = false;
            focusedAt = System.currentTimeMillis();
            return;
        }
        commitField();
        fieldOwner = owner;
        fieldText.setLength(0);
        fieldText.append(value == null ? "" : value);
        fieldMax = maxLength;
        fieldFresh = true;
        focusedAt = System.currentTimeMillis();
        if (textInput != null) textInput.accept(true);
    }

    /** Leaves the focused field, applying what was typed; text that does not parse is dropped. */
    void commitField() {
        if (fieldOwner == null) return;
        Object owner = fieldOwner;
        String text = fieldText.toString().trim();
        fieldOwner = null;
        fieldFresh = false;
        if (textInput != null) textInput.accept(false);
        if (owner instanceof Part) applyHex((Part) owner, text);
        else if (owner == NAME_FIELD) applyName(text);
    }

    /**
     * Sets a part's color from {@code #AARRGGBB} or {@code #RRGGBB} (the {@code #} optional);
     * six digits keep the part's opacity. A new color also turns the part on.
     */
    private void applyHex(Part part, String text) {
        HudBoxStyle style = style();
        if (style == null) return;
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6 && digits.length() != 8) return;
        long value;
        try {
            value = Long.parseLong(digits, 16);
        } catch (NumberFormatException e) {
            return;
        }
        int current = part.color(style);
        int color = digits.length() == 6 ? (current & 0xFF000000) | (int) value : (int) value;
        if (color == current) return;
        change(() -> {
            HudBoxStyle target = style();
            if (target == null) return;
            part.setColor(target, color);
            part.setOn(target, true);
        });
    }

    private void applyName(String text) {
        HudGroup group = group();
        if (group == null || text.isEmpty() || text.equals(group.getName())) return;
        change(() -> {
            HudGroup target = group();
            if (target != null) target.setName(text);
        });
    }

    /**
     * Edits the focused field: Backspace (a whole selection, or everything with Ctrl), select
     * all, and Enter or Escape to apply. Other keys are taken so nothing behind reacts.
     *
     * @return whether a field has focus and took the key
     */
    boolean fieldKey(int key, int modifiers) {
        if (fieldOwner == null) return false;
        boolean shortcut = (modifiers & Theme.GLFW_MOD_SHORTCUT) != 0;
        switch (key) {
            case Theme.GLFW_BACKSPACE:
            case Theme.GLFW_DELETE:
                if (fieldFresh || shortcut) fieldText.setLength(0);
                else if (fieldText.length() > 0) fieldText.setLength(fieldText.length() - 1);
                fieldFresh = false;
                return true;
            case Theme.GLFW_A:
                if (shortcut) fieldFresh = true;
                return true;
            case Theme.GLFW_ENTER:
            case Theme.GLFW_KP_ENTER:
            case Theme.GLFW_ESCAPE:
                commitField();
                return true;
            default:
                return true;
        }
    }
}
