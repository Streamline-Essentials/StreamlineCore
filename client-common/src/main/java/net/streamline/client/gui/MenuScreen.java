package net.streamline.client.gui;

import net.streamline.client.Canvas;
import net.streamline.client.compat.ClientCompat;
import net.streamline.client.compat.CompatScreen;
import singularity.client.hud.HudPlacement;
import singularity.client.hud.HudRegistry;
import singularity.client.module.ClientModuleCloud;
import singularity.client.module.ClientModuleManager;
import singularity.client.settings.ClientSettingsRegistry;
import singularity.client.settings.ModuleSettings;
import singularity.client.settings.Setting;
import singularity.registry.ModuleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The client module menu ({@code /slclient menu}): a sidebar of the client modules loaded in
 * this game and, for the selected one, its details, an enable switch, reload, update and
 * remove buttons, the settings it registered with {@link ClientSettingsRegistry}, and its HUD
 * elements. The sidebar's last entry browses eCloud for client modules and installs them
 * while the game runs.
 *
 * <p>Everything is drawn by hand so it looks the same on every Minecraft version. Each frame
 * records the clickable areas it drew in {@link #regions}; input is matched against the
 * previous frame's areas, which is what the player saw when they clicked.</p>
 */
public final class MenuScreen extends CompatScreen {

    private static final int SIDEBAR_WIDTH = 116;
    private static final int HEADER_HEIGHT = 22;
    private static final int PAD = 8;
    private static final int SCROLL_STEP = 16;

    /** The screen to return to: a version-specific screen object, or {@code null}. */
    private final Object parent;
    private final List<Region> regions = new ArrayList<>();
    /** Sidebar key of the eCloud browser; never a Plugin-Id, which cannot contain spaces. */
    private static final String BROWSE = " ecloud";

    /** Modules (by id) with an action running in the background. */
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    /** The last action's outcome per module, shown under its controls; red when it starts with "!". */
    private final Map<String, String> notices = new ConcurrentHashMap<>();
    /** The module whose Remove button was clicked once; a second click removes it. */
    private String confirmRemove;

    /** eCloud's client modules, once fetched; {@code null} until then. */
    private volatile List<ModuleRegistry.ModuleInfo> available;
    private volatile String availableError;
    private volatile boolean loadingAvailable;

    private String selected;
    private double contentScroll;
    private double sidebarScroll;
    private int contentOverflow;
    private int sidebarOverflow;
    private int contentX1, contentY1, contentX2, contentY2;
    private int sidebarX1, sidebarY1, sidebarX2, sidebarY2;

    private SliderDrag sliderDrag;
    private TextEdit editing;
    private long openedAt = System.currentTimeMillis();

    public MenuScreen(Object parent) {
        super("Streamline Client");
        this.parent = parent;
    }

    /** A clickable area and what clicking it does. */
    private interface Click {
        void click(double x, double y, int button);
    }

    private static final class Region {
        final int x1, y1, x2, y2;
        final Click click;
        final Object owner;

        Region(int x1, int y1, int x2, int y2, Object owner, Click click) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.owner = owner;
            this.click = click;
        }
    }

    private static final class SliderDrag {
        final ModuleSettings page;
        final Setting.Slider setting;
        final int trackX1;
        final int trackX2;

        SliderDrag(ModuleSettings page, Setting.Slider setting, int trackX1, int trackX2) {
            this.page = page;
            this.setting = setting;
            this.trackX1 = trackX1;
            this.trackX2 = trackX2;
        }

        void moveTo(double x) {
            double fraction = Math.max(0, Math.min(1, (x - trackX1) / (double) Math.max(1, trackX2 - trackX1)));
            setting.set(setting.getMin() + fraction * (setting.getMax() - setting.getMin()));
        }
    }

    /** A text or color field being typed into; the value is applied on Enter or focus loss. */
    private static final class TextEdit {
        final ModuleSettings page;
        final Setting<?> setting;
        final StringBuilder buffer;

        TextEdit(ModuleSettings page, Setting<?> setting) {
            this.page = page;
            this.setting = setting;
            this.buffer = new StringBuilder(setting.getKind() == Setting.Kind.COLOR
                    ? setting.formatValue() : String.valueOf(setting.getValue()));
        }

        int maxLength() {
            if (setting instanceof Setting.Text) return ((Setting.Text) setting).getMaxLength();
            return 9;
        }
    }

    // ---------------------------------------------------------------- data

    private static String displayName(ClientModuleManager.Installed module) {
        ModuleSettings page = ClientSettingsRegistry.get(module.getId());
        if (page != null && ! page.getDisplayName().isEmpty()) return page.getDisplayName();
        return module.getName();
    }

    /** eCloud's entry for an installed module, once the list has been fetched. */
    private ModuleRegistry.ModuleInfo listed(String id) {
        List<ModuleRegistry.ModuleInfo> list = available;
        if (list == null) return null;
        for (ModuleRegistry.ModuleInfo info : list) {
            if (info.getId().equalsIgnoreCase(id)) return info;
        }
        return null;
    }

    private void fetchAvailable() {
        if (loadingAvailable) return;
        loadingAvailable = true;
        availableError = null;
        ClientModuleCloud.available().whenComplete((list, error) -> {
            if (error == null) available = list;
            else availableError = message(error);
            loadingAvailable = false;
        });
    }

    private static String message(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void draw(Canvas canvas, int mouseX, int mouseY, float partialTick) {
        regions.clear();
        canvas.fill(0, 0, canvas.width(), canvas.height(), Theme.BACKDROP);

        int width = Math.min(canvas.width() - 20, 480);
        int height = Math.min(canvas.height() - 20, 300);
        int left = (canvas.width() - width) / 2;
        int top = (canvas.height() - height) / 2;
        canvas.fill(left, top, left + width, top + height, Theme.PANEL);

        drawHeader(canvas, mouseX, mouseY, left, top, width);

        sidebarX1 = left;
        sidebarY1 = top + HEADER_HEIGHT;
        sidebarX2 = left + SIDEBAR_WIDTH;
        sidebarY2 = top + height;
        contentX1 = sidebarX2;
        contentY1 = sidebarY1;
        contentX2 = left + width;
        contentY2 = top + height;

        List<ClientModuleManager.Installed> modules = ClientModuleManager.modules();
        if (selected == null || (! BROWSE.equals(selected) && modules.stream().noneMatch(m -> m.getId().equals(selected)))) {
            selected = modules.isEmpty() ? BROWSE : modules.get(0).getId();
            contentScroll = 0;
        }
        if (available == null && availableError == null) fetchAvailable();

        drawSidebar(canvas, mouseX, mouseY, modules);
        drawContent(canvas, mouseX, mouseY, modules);
    }

    private void drawHeader(Canvas canvas, int mouseX, int mouseY, int left, int top, int width) {
        canvas.fill(left, top, left + width, top + HEADER_HEIGHT, Theme.HEADER);
        canvas.text("&lStreamline &rClient", left + PAD, top + 7, 0xFFFFFFFF, true);

        int doneX2 = left + width - 6;
        int doneX1 = doneX2 - 44;
        boolean doneHover = Theme.inside(mouseX, mouseY, doneX1, top + 4, doneX2, top + 18);
        Theme.button(canvas, doneX1, top + 4, doneX2, top + 18, "Done", doneHover, false);
        regions.add(new Region(doneX1, top + 4, doneX2, top + 18, null, (x, y, b) -> onClose()));

        // Header buttons use the dark red: the bright one would vanish into the red header.
        int hudX2 = doneX1 - 6;
        int hudX1 = hudX2 - 74;
        boolean hudHover = Theme.inside(mouseX, mouseY, hudX1, top + 4, hudX2, top + 18);
        Theme.button(canvas, hudX1, top + 4, hudX2, top + 18, "HUD Layout", hudHover, false);
        regions.add(new Region(hudX1, top + 4, hudX2, top + 18, null, (x, y, b) -> openHudEditor()));
    }

    private void drawSidebar(Canvas canvas, int mouseX, int mouseY, List<ClientModuleManager.Installed> modules) {
        canvas.fill(sidebarX1, sidebarY1, sidebarX2, sidebarY2, Theme.SIDEBAR);
        int rowHeight = 18;
        int total = (modules.size() + 1) * rowHeight + 12;
        sidebarOverflow = Math.max(0, total - (sidebarY2 - sidebarY1));
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, sidebarOverflow));

        canvas.clip(sidebarX1, sidebarY1, sidebarX2, sidebarY2);
        int y = sidebarY1 + 4 - (int) sidebarScroll;
        for (ClientModuleManager.Installed module : modules) {
            int dot = module.isEnabled() ? 0xFF4CC26A : 0xFF6A6F77;
            sidebarRow(canvas, mouseX, mouseY, module.getId(), displayName(module), dot, y, rowHeight);
            y += rowHeight;
        }
        y += 4;
        canvas.fill(sidebarX1 + 6, y - 2, sidebarX2 - 6, y - 1, Theme.CONTROL_BORDER);
        sidebarRow(canvas, mouseX, mouseY, BROWSE, "Browse eCloud", Theme.ACCENT_LIGHT, y, rowHeight);
        canvas.endClip();
    }

    private void sidebarRow(Canvas canvas, int mouseX, int mouseY, String key, String label, int dot, int y, int rowHeight) {
        boolean isSelected = key.equals(selected);
        boolean visible = y + rowHeight > sidebarY1 && y < sidebarY2;
        boolean hover = visible && Theme.inside(mouseX, mouseY, sidebarX1, Math.max(y, sidebarY1), sidebarX2, Math.min(y + rowHeight, sidebarY2));
        if (isSelected) {
            canvas.fill(sidebarX1, y, sidebarX2, y + rowHeight, 0x30FFFFFF);
            canvas.fill(sidebarX1, y, sidebarX1 + 2, y + rowHeight, Theme.ACCENT);
        } else if (hover) {
            canvas.fill(sidebarX1, y, sidebarX2, y + rowHeight, Theme.HOVER);
        }
        canvas.fill(sidebarX1 + 7, y + 7, sidebarX1 + 11, y + 11, dot);
        canvas.text(Theme.fit(canvas, label, SIDEBAR_WIDTH - 22), sidebarX1 + 16, y + 5,
                isSelected ? Theme.TEXT : Theme.MUTED);
        if (visible) {
            regions.add(new Region(sidebarX1, Math.max(y, sidebarY1), sidebarX2, Math.min(y + rowHeight, sidebarY2), null, (x, yy, b) -> {
                if (! key.equals(selected)) {
                    commitEdit();
                    selected = key;
                    contentScroll = 0;
                    confirmRemove = null;
                }
            }));
        }
    }

    private void drawContent(Canvas canvas, int mouseX, int mouseY, List<ClientModuleManager.Installed> modules) {
        int textWidth = contentX2 - contentX1 - PAD * 2;
        canvas.clip(contentX1, contentY1, contentX2, contentY2);
        int y = contentY1 + PAD - (int) contentScroll;
        int start = y;

        ClientModuleManager.Installed module = modules.stream().filter(m -> m.getId().equals(selected)).findFirst().orElse(null);
        if (module == null) y = drawBrowse(canvas, mouseX, mouseY, modules, y, textWidth);
        else y = drawModule(canvas, mouseX, mouseY, module, y, textWidth);

        canvas.endClip();
        contentOverflow = Math.max(0, (y - start) + PAD - (contentY2 - contentY1));
        contentScroll = Math.max(0, Math.min(contentScroll, contentOverflow));
        if (contentOverflow > 0) {
            int track = contentY2 - contentY1;
            int barHeight = Math.max(12, track * track / (track + contentOverflow));
            int barY = contentY1 + (int) ((track - barHeight) * (contentScroll / contentOverflow));
            canvas.fill(contentX2 - 3, barY, contentX2 - 1, barY + barHeight, 0x60FFFFFF);
        }
    }

    private int paragraph(Canvas canvas, String text, int y, int width, int color) {
        for (String line : Theme.wrap(canvas, text, width)) {
            canvas.text(line, contentX1 + PAD, y, color, false);
            y += canvas.lineHeight() + 2;
        }
        return y;
    }

    private boolean visible(int y1, int y2) {
        return y2 > contentY1 && y1 < contentY2;
    }

    private void addRegion(int x1, int y1, int x2, int y2, Object owner, Click click) {
        if (! visible(y1, y2)) return;
        regions.add(new Region(x1, Math.max(y1, contentY1), x2, Math.min(y2, contentY2), owner, click));
    }

    private boolean hovered(int mouseX, int mouseY, int x1, int y1, int x2, int y2) {
        return visible(y1, y2) && Theme.inside(mouseX, mouseY, x1, Math.max(y1, contentY1), x2, Math.min(y2, contentY2));
    }

    private int drawModule(Canvas canvas, int mouseX, int mouseY, ClientModuleManager.Installed module, int y, int textWidth) {
        String id = module.getId();
        int x1 = contentX1 + PAD;
        int right = contentX2 - PAD - 4;

        String name = displayName(module);
        canvas.text("&l" + name, x1, y, Theme.TEXT, false);
        canvas.text("v" + module.getVersion(), x1 + canvas.textWidth("&l" + name) + 6, y, Theme.MUTED);
        y += canvas.lineHeight() + 3;
        if (! module.getProvider().isEmpty()) {
            canvas.text(Theme.fit(canvas, "by " + module.getProvider(), textWidth), x1, y, Theme.MUTED);
            y += canvas.lineHeight() + 3;
        }
        if (! module.getDescription().isEmpty()) {
            y = paragraph(canvas, module.getDescription(), y, textWidth, Theme.MUTED);
        }
        y += 4;
        y = drawModuleActions(canvas, mouseX, mouseY, module, y, x1);
        y += 6;

        // Enable switch.
        canvas.fill(x1 - 2, y - 3, right + 2, y + 13, 0x10FFFFFF);
        canvas.text("Enabled", x1, y + 1, Theme.TEXT);
        if (pending.contains(id)) {
            canvas.text("...", right - canvas.textWidth("..."), y + 1, Theme.MUTED);
        } else {
            boolean hover = hovered(mouseX, mouseY, right - 20, y, right, y + 10);
            Theme.toggle(canvas, right - 20, y, module.isEnabled(), hover);
            addRegion(right - 20, y, right, y + 10, null, (cx, cy, b) -> toggleModule(id, module.isEnabled()));
        }
        y += 20;

        ModuleSettings page = ClientSettingsRegistry.get(id);
        if (! module.isEnabled()) {
            y = paragraph(canvas, "Enable the module to change its settings.", y, textWidth, Theme.MUTED);
        } else if (page != null && ! page.getSettings().isEmpty()) {
            y = section(canvas, "Settings", y, right);
            for (Setting<?> setting : page.getSettings()) {
                y = drawSetting(canvas, mouseX, mouseY, page, setting, y, x1, right);
            }
        }

        List<HudRegistry.Registered> elements = HudRegistry.elementsOf(id);
        if (! elements.isEmpty()) {
            y += 4;
            y = section(canvas, "HUD elements", y, right);
            for (HudRegistry.Registered registered : elements) {
                HudPlacement placement = registered.getPlacement();
                if (hovered(mouseX, mouseY, x1 - 2, y - 2, right + 2, y + 12)) canvas.fill(x1 - 2, y - 2, right + 2, y + 12, Theme.HOVER);
                canvas.text(Theme.fit(canvas, registered.getElement().getDisplayName(), right - x1 - 30), x1, y + 1, Theme.TEXT);
                boolean hover = hovered(mouseX, mouseY, right - 20, y, right, y + 10);
                Theme.toggle(canvas, right - 20, y, placement.isEnabled(), hover);
                addRegion(right - 20, y, right, y + 10, null, (cx, cy, b) -> {
                    placement.setEnabled(! placement.isEnabled());
                    HudRegistry.save();
                });
                y += 16;
            }
            y += 2;
            int buttonX2 = x1 + 100;
            boolean hover = hovered(mouseX, mouseY, x1, y, buttonX2, y + 14);
            Theme.button(canvas, x1, y, buttonX2, y + 14, "Edit HUD layout", hover, true);
            addRegion(x1, y, buttonX2, y + 14, null, (cx, cy, b) -> openHudEditor());
            y += 18;
        }
        return y;
    }

    private int section(Canvas canvas, String title, int y, int right) {
        int x1 = contentX1 + PAD;
        canvas.text(title.toUpperCase(Locale.ROOT), x1, y, Theme.ACCENT);
        canvas.fill(x1, y + canvas.lineHeight() + 1, right, y + canvas.lineHeight() + 2, Theme.CONTROL_BORDER);
        return y + canvas.lineHeight() + 6;
    }

    private int drawSetting(Canvas canvas, int mouseX, int mouseY, ModuleSettings page, Setting<?> setting, int y, int x1, int right) {
        boolean hasDescription = ! setting.getDescription().isEmpty();
        int rowHeight = hasDescription ? 24 : 16;
        if (hovered(mouseX, mouseY, x1 - 2, y - 2, right + 2, y + rowHeight - 2)) {
            canvas.fill(x1 - 2, y - 2, right + 2, y + rowHeight - 2, Theme.HOVER);
        }

        int controlWidth = controlWidth(setting);
        int labelWidth = right - x1 - controlWidth - 8;
        canvas.text(Theme.fit(canvas, setting.getDisplayName(), labelWidth), x1, y + 1, Theme.TEXT);
        if (hasDescription) {
            canvas.text(Theme.fit(canvas, setting.getDescription(), labelWidth), x1, y + 11, Theme.MUTED);
        }

        int cx2 = right;
        int cx1 = right - controlWidth;
        switch (setting.getKind()) {
            case TOGGLE: {
                Setting.Toggle toggle = (Setting.Toggle) setting;
                boolean on = Boolean.TRUE.equals(toggle.getValue());
                Theme.toggle(canvas, cx2 - 20, y, on, hovered(mouseX, mouseY, cx2 - 20, y, cx2, y + 10));
                addRegion(cx2 - 20, y, cx2, y + 10, setting, (x, yy, b) -> {
                    toggle.set(! Boolean.TRUE.equals(toggle.getValue()));
                    page.save();
                });
                break;
            }
            case SLIDER: {
                Setting.Slider slider = (Setting.Slider) setting;
                String value = formatNumber(slider.getValue(), slider.getStep());
                int trackX1 = cx2 - 90;
                canvas.text(value, trackX1 - 6 - canvas.textWidth(value), y + 1, Theme.MUTED);
                canvas.fill(trackX1, y + 4, cx2, y + 6, Theme.SWITCH_OFF);
                double range = slider.getMax() - slider.getMin();
                double fraction = range <= 0 ? 0 : (slider.getValue() - slider.getMin()) / range;
                int knobX = trackX1 + (int) Math.round(fraction * (cx2 - trackX1 - 4));
                canvas.fill(trackX1, y + 4, knobX + 2, y + 6, Theme.ACCENT);
                boolean dragging = sliderDrag != null && sliderDrag.setting == slider;
                canvas.fill(knobX, y + 1, knobX + 4, y + 9, dragging || hovered(mouseX, mouseY, trackX1, y, cx2, y + 10) ? 0xFFFFFFFF : 0xFFD0D0D0);
                addRegion(trackX1, y - 1, cx2, y + 11, setting, (x, yy, b) -> {
                    sliderDrag = new SliderDrag(page, slider, trackX1, cx2);
                    sliderDrag.moveTo(x);
                });
                break;
            }
            case CHOICE: {
                Setting.Choice choice = (Setting.Choice) setting;
                boolean hover = hovered(mouseX, mouseY, cx1, y - 1, cx2, y + 11);
                canvas.fill(cx1, y - 1, cx2, y + 11, hover ? Theme.CONTROL_BORDER : Theme.CONTROL);
                String label = Theme.fit(canvas, String.valueOf(choice.getValue()), controlWidth - 20);
                canvas.text("<", cx1 + 3, y + 1, Theme.MUTED);
                canvas.text(">", cx2 - 3 - canvas.textWidth(">"), y + 1, Theme.MUTED);
                canvas.text(label, cx1 + (controlWidth - canvas.textWidth(label)) / 2, y + 1, Theme.TEXT);
                addRegion(cx1, y - 1, cx2, y + 11, setting, (x, yy, b) -> {
                    List<String> options = choice.getOptions();
                    if (options.isEmpty()) return;
                    int index = options.indexOf(choice.getValue());
                    boolean back = b == 1 || x < cx1 + controlWidth / 3.0;
                    index = Math.floorMod(index + (back ? -1 : 1), options.size());
                    choice.set(options.get(index));
                    page.save();
                });
                break;
            }
            case TEXT:
            case COLOR: {
                boolean isColor = setting.getKind() == Setting.Kind.COLOR;
                int fieldX1 = isColor ? cx1 + 16 : cx1;
                if (isColor) {
                    int color = setting.getValue() instanceof Integer ? (Integer) setting.getValue() : 0xFFFFFFFF;
                    canvas.fill(cx1, y - 1, cx1 + 12, y + 11, Theme.CONTROL_BORDER);
                    canvas.fill(cx1 + 1, y, cx1 + 11, y + 10, Canvas.opaque(color));
                }
                boolean focused = editing != null && editing.setting == setting;
                boolean hover = hovered(mouseX, mouseY, fieldX1, y - 1, cx2, y + 11);
                canvas.fill(fieldX1, y - 1, cx2, y + 11, Theme.CONTROL);
                canvas.outline(fieldX1, y - 1, cx2, y + 11, focused ? Theme.ACCENT : (hover ? Theme.MUTED : Theme.CONTROL_BORDER));
                String shown = focused ? editing.buffer.toString() : (isColor ? setting.formatValue() : String.valueOf(setting.getValue()));
                String visibleText = fitTail(canvas, shown, cx2 - fieldX1 - 8);
                canvas.text(visibleText, fieldX1 + 3, y + 1, Theme.TEXT);
                boolean caret = focused && ((System.currentTimeMillis() - openedAt) / 500) % 2 == 0;
                if (caret) {
                    int caretX = fieldX1 + 3 + canvas.textWidth(visibleText);
                    canvas.fill(caretX, y, caretX + 1, y + 10, Theme.TEXT);
                }
                addRegion(fieldX1, y - 1, cx2, y + 11, setting, (x, yy, b) -> {
                    if (editing == null || editing.setting != setting) {
                        commitEdit();
                        editing = new TextEdit(page, setting);
                        openedAt = System.currentTimeMillis();
                    }
                });
                break;
            }
            default:
                break;
        }
        return y + rowHeight;
    }

    private static int controlWidth(Setting<?> setting) {
        switch (setting.getKind()) {
            case TOGGLE:
                return 20;
            case SLIDER:
                return 120;
            case CHOICE:
                return 90;
            case COLOR:
                return 86;
            case TEXT:
            default:
                return 120;
        }
    }

    /** Keeps the end of the text, where the caret is, when it is too wide for its field. */
    private static String fitTail(Canvas canvas, String text, int width) {
        if (canvas.textWidth(text) <= width) return text;
        int start = 0;
        while (start < text.length() && canvas.textWidth(text.substring(start)) > width) start++;
        return text.substring(start);
    }

    private static String formatNumber(Double value, double step) {
        if (value == null) return "";
        if (step >= 1 && Math.rint(step) == step) return String.valueOf(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // ---------------------------------------------------------------- actions

    /** Reload, Update (when eCloud lists a newer version) and Remove, then the last action's outcome. */
    private int drawModuleActions(Canvas canvas, int mouseX, int mouseY, ClientModuleManager.Installed module, int y, int x1) {
        String id = module.getId();
        if (pending.contains(id)) {
            canvas.text("Working...", x1, y + 3, Theme.MUTED);
            return y + 18;
        }
        int x = actionButton(canvas, mouseX, mouseY, x1, y, "Reload", false,
                () -> run(id, ClientModuleManager.reload(id), m -> "Reloaded " + m.getVersion() + "."));

        ModuleRegistry.ModuleInfo listed = listed(id);
        if (listed != null && ModuleRegistry.compareVersions(module.getVersion(), listed.getVersion()) < 0) {
            x = actionButton(canvas, mouseX, mouseY, x, y, "Update to " + listed.getVersion(), true,
                    () -> run(id, ClientModuleCloud.update(id), result -> "Updated to " + result.getVersion() + "."));
        }

        boolean confirming = id.equals(confirmRemove);
        actionButton(canvas, mouseX, mouseY, x, y, confirming ? "Click again to remove" : "Remove", confirming, () -> {
            if (! id.equals(confirmRemove)) {
                confirmRemove = id;
                return;
            }
            confirmRemove = null;
            run(id, ClientModuleManager.remove(id), jar -> "Removed " + jar + ".");
        });
        return notice(canvas, id, y + 18, x1, contentX2 - PAD - 4);
    }

    /** @return the x where the next button goes */
    private int actionButton(Canvas canvas, int mouseX, int mouseY, int x, int y, String label, boolean primary, Runnable action) {
        int x2 = x + canvas.textWidth(label) + 14;
        Theme.button(canvas, x, y, x2, y + 14, label, hovered(mouseX, mouseY, x, y, x2, y + 14), primary);
        addRegion(x, y, x2, y + 14, null, (cx, cy, b) -> action.run());
        return x2 + 6;
    }

    /** Draws the last action's outcome for {@code key}, if any. */
    private int notice(Canvas canvas, String key, int y, int x1, int right) {
        String notice = notices.get(key);
        if (notice == null) return y;
        boolean failed = notice.startsWith("!");
        for (String line : Theme.wrap(canvas, failed ? notice.substring(1) : notice, right - x1)) {
            canvas.text(line, x1, y, failed ? Theme.ACCENT_LIGHT : 0xFF4CC26A);
            y += canvas.lineHeight() + 2;
        }
        return y;
    }

    /**
     * Runs a background action for one module, keeping its controls busy until it ends and
     * noting the outcome. Module code runs off the render thread, on the client module worker.
     */
    private <T> void run(String key, CompletableFuture<T> action, Function<T, String> success) {
        if (! pending.add(key)) return;
        notices.remove(key);
        action.whenComplete((value, error) -> {
            if (error == null) notices.put(key, success.apply(value));
            else notices.put(key, "!" + message(error));
            pending.remove(key);
        });
    }

    private void toggleModule(String id, boolean enabled) {
        if (enabled) run(id, ClientModuleManager.disable(id), m -> "Disabled. It stays off until you enable it.");
        else run(id, ClientModuleManager.enable(id), m -> "Enabled.");
    }

    /** The eCloud page: every client module the registry lists, with install and update buttons. */
    private int drawBrowse(Canvas canvas, int mouseX, int mouseY, List<ClientModuleManager.Installed> modules, int y, int textWidth) {
        int x1 = contentX1 + PAD;
        int right = contentX2 - PAD - 4;
        canvas.text("&lBrowse eCloud", x1, y, Theme.TEXT, false);
        if (! loadingAvailable) {
            int refreshX1 = right - canvas.textWidth("Refresh") - 14;
            Theme.button(canvas, refreshX1, y - 3, right, y + 11, "Refresh", hovered(mouseX, mouseY, refreshX1, y - 3, right, y + 11), false);
            addRegion(refreshX1, y - 3, right, y + 11, null, (cx, cy, b) -> fetchAvailable());
        }
        y += canvas.lineHeight() + 5;
        y = paragraph(canvas, "Client modules from eCloud. They install and start right away; no restart needed.",
                y, textWidth, Theme.MUTED);
        if (modules.isEmpty()) {
            y = paragraph(canvas, "No client modules are installed yet. Jars put in " + ClientModuleManager.folder()
                    + " also load as soon as they land.", y, textWidth, Theme.MUTED);
        }
        for (String reason : ClientModuleManager.refusals().values()) {
            y = paragraph(canvas, reason, y, textWidth, Theme.ACCENT_LIGHT);
        }
        y += 6;

        List<ModuleRegistry.ModuleInfo> list = available;
        if (availableError != null) {
            y = paragraph(canvas, "Could not reach eCloud: " + availableError, y, textWidth, Theme.ACCENT_LIGHT);
        }
        if (list == null) return loadingAvailable ? paragraph(canvas, "Loading...", y, textWidth, Theme.MUTED) : y;
        if (list.isEmpty()) return paragraph(canvas, "eCloud lists no client modules yet.", y, textWidth, Theme.MUTED);

        for (ModuleRegistry.ModuleInfo info : list) {
            y = drawListing(canvas, mouseX, mouseY, info, y, x1, right);
        }
        return y;
    }

    private int drawListing(Canvas canvas, int mouseX, int mouseY, ModuleRegistry.ModuleInfo info, int y, int x1, int right) {
        int top = y;
        String key = info.getId().isEmpty() ? info.getName() : info.getId();
        ClientModuleManager.Installed installed = ClientModuleManager.module(key);
        boolean outdated = installed != null && ModuleRegistry.compareVersions(installed.getVersion(), info.getVersion()) < 0;

        String label;
        Runnable action = null;
        if (pending.contains(key)) {
            label = "Working...";
        } else if (installed == null) {
            label = "Install";
            action = () -> run(key, ClientModuleCloud.download(key, null), result -> "Installed " + result.getVersion() + ".");
        } else if (outdated) {
            label = "Update";
            action = () -> run(key, ClientModuleCloud.update(key), result -> "Updated to " + result.getVersion() + ".");
        } else {
            label = "Installed";
        }
        int buttonX1 = right - Math.max(56, canvas.textWidth(label) + 14);
        int textWidth = buttonX1 - x1 - 8;

        if (hovered(mouseX, mouseY, x1 - 2, top - 3, right + 2, top + canvas.lineHeight() * 2 + 6)) {
            canvas.fill(x1 - 2, top - 3, right + 2, top + canvas.lineHeight() * 2 + 6, Theme.HOVER);
        }
        canvas.text(Theme.fit(canvas, "&l" + info.getName() + " &r&7v" + info.getVersion(), textWidth), x1, y + 1, Theme.TEXT);
        if (action != null) {
            Runnable click = action;
            Theme.button(canvas, buttonX1, y - 2, right, y + 12, label, hovered(mouseX, mouseY, buttonX1, y - 2, right, y + 12), true);
            addRegion(buttonX1, y - 2, right, y + 12, null, (cx, cy, b) -> click.run());
        } else {
            canvas.text(label, right - canvas.textWidth(label), y + 1, Theme.MUTED);
        }
        y += canvas.lineHeight() + 3;

        String byline = (info.getAuthor().isEmpty() ? "" : "by " + info.getAuthor() + "  ")
                + info.getDownloads() + (info.getDownloads() == 1 ? " download" : " downloads")
                + (installed != null ? "  (installed " + installed.getVersion() + ")" : "");
        canvas.text(Theme.fit(canvas, byline, textWidth), x1, y, Theme.MUTED);
        y += canvas.lineHeight() + 2;
        for (String line : Theme.wrap(canvas, info.getDescription(), right - x1)) {
            canvas.text(line, x1, y, Theme.MUTED);
            y += canvas.lineHeight() + 2;
        }
        y = notice(canvas, key, y, x1, right);
        y += 4;
        canvas.fill(x1, y, right, y + 1, Theme.CONTROL_BORDER);
        return y + 7;
    }

    private void openHudEditor() {
        commitEdit();
        ClientCompat.setScreen(new HudEditorScreen(this));
    }

    private void commitEdit() {
        if (editing == null) return;
        TextEdit edit = editing;
        editing = null;
        edit.setting.setFromText(edit.buffer.toString());
        edit.page.save();
    }

    // ---------------------------------------------------------------- input

    @Override
    protected boolean mouseDown(double x, double y, int button) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region region = regions.get(i);
            if (! Theme.inside(x, y, region.x1, region.y1, region.x2, region.y2)) continue;
            if (editing != null && region.owner != editing.setting) commitEdit();
            region.click.click(x, y, button);
            return true;
        }
        commitEdit();
        return false;
    }

    @Override
    protected boolean mouseDrag(double x, double y, int button, double dx, double dy) {
        if (sliderDrag == null) return false;
        sliderDrag.moveTo(x);
        return true;
    }

    @Override
    protected boolean mouseUp(double x, double y, int button) {
        if (sliderDrag == null) return false;
        sliderDrag.page.save();
        sliderDrag = null;
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        if (Theme.inside(x, y, sidebarX1, sidebarY1, sidebarX2, sidebarY2)) {
            sidebarScroll = Math.max(0, Math.min(sidebarOverflow, sidebarScroll - amount * SCROLL_STEP));
            return true;
        }
        if (Theme.inside(x, y, contentX1, contentY1, contentX2, contentY2)) {
            contentScroll = Math.max(0, Math.min(contentOverflow, contentScroll - amount * SCROLL_STEP));
            return true;
        }
        return false;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (editing == null) return false;
        switch (key) {
            case Theme.GLFW_BACKSPACE:
                if (editing.buffer.length() > 0) editing.buffer.setLength(editing.buffer.length() - 1);
                return true;
            case Theme.GLFW_ENTER:
            case Theme.GLFW_KP_ENTER:
                commitEdit();
                return true;
            case Theme.GLFW_ESCAPE:
                editing = null;
                return true;
            default:
                // Swallow other keys so typing does not trigger keybinds behind the menu.
                return true;
        }
    }

    @Override
    protected boolean typed(String text) {
        if (editing == null || text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c == 127) continue;
            if (editing.buffer.length() >= editing.maxLength()) break;
            editing.buffer.append(c);
        }
        return true;
    }

    @Override
    public void onClose() {
        commitEdit();
        ClientCompat.setScreen(parent);
    }
}
