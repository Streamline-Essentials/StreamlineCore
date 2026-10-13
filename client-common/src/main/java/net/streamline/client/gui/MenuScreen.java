package net.streamline.client.gui;

import net.streamline.client.Canvas;
import net.streamline.client.CanvasHudRenderer;
import net.streamline.client.compat.ClientCompat;
import net.streamline.client.compat.CompatScreen;
import singularity.client.hud.CustomHudElements;
import singularity.client.hud.HudElement;
import singularity.client.hud.HudPlacement;
import singularity.client.hud.HudRegistry;
import singularity.client.module.ClientModuleCloud;
import singularity.client.module.ClientModuleManager;
import singularity.client.module.WorldModules;
import singularity.client.placeholders.ClientPlaceholders;
import singularity.client.settings.ClientSettingsRegistry;
import singularity.client.settings.ModuleSettings;
import singularity.client.settings.Setting;
import singularity.client.settings.StreamlineClientSettings;
import singularity.registry.ModuleRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The client module menu ({@code /slclient menu}): a sidebar of the client modules loaded in
 * this game and, for the selected one, its details, an enable switch, reload, update and
 * remove buttons, the settings it registered with {@link ClientSettingsRegistry}, and its HUD
 * elements, whose text can be edited where the element allows it, plus an add button for
 * modules that let the player add text elements of their own. The sidebar's first entry,
 * Client Settings, holds StreamlineClient's own settings ({@link StreamlineClientSettings}); its last entry browses eCloud for client modules, and for world modules
 * (server modules that run in the worlds this client hosts), and installs them
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
    private static final int TEMPLATE_MAX_LENGTH = 256;
    private static final int ID_MAX_LENGTH = 32;

    /** The screen to return to: a version-specific screen object, or {@code null}. */
    private final Object parent;
    private final List<Region> regions = new ArrayList<>();
    /** Sidebar key of the eCloud browser; never a Plugin-Id, which cannot contain spaces. */
    private static final String BROWSE = " ecloud";
    /** Sidebar key of the Client Settings page; never a Plugin-Id either. */
    private static final String CLIENT = " client";
    /** The tag pill on eCloud entries for world modules. */
    private static final String WORLD_TAG = "world";
    /** Stands for the {@link #WORLD_TAG} pill inside text drawn by {@link #taggedParagraph}. */
    private static final String WORLD_TAG_TOKEN = "[" + WORLD_TAG + "]";
    /** The page the menu was last on, reopened when Remember Last Page is on. */
    private static volatile String lastSelected;

    /** Modules (by id) with an action running in the background. */
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    /** The last action's outcome per module, shown under its controls; red when it starts with "!". */
    private final Map<String, String> notices = new ConcurrentHashMap<>();
    /**
     * When each short-lived notice disappears (epoch millis), by the same key as {@link #notices}.
     * Short-lived notices also disappear when the menu switches pages.
     */
    private final Map<String, Long> noticeExpiry = new ConcurrentHashMap<>();
    /** How long a successful update's notice stays up. */
    private static final long UPDATED_NOTICE_MILLIS = 3000L;
    /** The module whose Remove button was clicked once; a second click removes it. */
    private String confirmRemove;

    /** eCloud's modules for clients, once fetched; {@code null} until then. */
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
    /**
     * Whether the left button went down in the focused field and is still held: each frame then
     * moves the caret to the pointer, selecting from where the press was.
     */
    private boolean selectingText;
    /** The key setting waiting for the player to press its key, and the page it saves to. */
    private Setting.Key capturing;
    private ModuleSettings capturingPage;
    private long openedAt = System.currentTimeMillis();

    /** The HUD element (registry key) whose text editor is open. */
    private String expandedElement;
    /** The HUD element (registry key) whose Delete button was clicked once. */
    private String confirmDelete;
    /** The module whose add-element form is open, and what has been typed into it. */
    private String addingFor;
    /** The module whose details popup is open, or {@code null}. */
    private String detailsFor;
    /** The eCloud entry (its key on the Browse eCloud page) whose details popup is open, or {@code null}. */
    private String listingDetailsFor;
    private String addId = "";
    private String addText = "";
    /** Owners of the add form's two fields, so input can tell them apart. */
    private final Object addIdField = new Object();
    private final Object addTextField = new Object();

    public MenuScreen(Object parent) {
        super("Streamline Client");
        this.parent = parent;
        if (StreamlineClientSettings.rememberPage()) selected = lastSelected;
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

    /**
     * A text field being typed into; the value is applied on Enter or focus loss. The owner
     * identifies the field across frames: a setting, a HUD element, or one of the add form's fields.
     *
     * <p>The selection runs between {@link #anchor} and {@link #caret}; when the two are equal
     * nothing is selected. Typing, pasting and deleting replace the selection.</p>
     */
    private static final class TextEdit {
        final Object owner;
        final StringBuilder buffer;
        final int maxLength;
        final Consumer<String> apply;
        /** Index in {@link #buffer} where typing goes; starts at the end. */
        int caret;
        /** The fixed end of the selection; the caret is the end that moves. */
        int anchor;

        TextEdit(Object owner, String initial, int maxLength, Consumer<String> apply) {
            this.owner = owner;
            this.buffer = new StringBuilder(initial == null ? "" : initial);
            this.maxLength = maxLength;
            this.apply = apply;
            this.caret = buffer.length();
            this.anchor = caret;
        }

        boolean hasSelection() {
            return anchor != caret;
        }

        int selectionStart() {
            return Math.min(anchor, caret);
        }

        int selectionEnd() {
            return Math.max(anchor, caret);
        }

        String selected() {
            return buffer.substring(selectionStart(), selectionEnd());
        }

        /** Removes the selected text, leaving the caret where it started. */
        void deleteSelection() {
            int start = selectionStart();
            buffer.delete(start, selectionEnd());
            caret = start;
            anchor = start;
        }

        void selectAll() {
            anchor = 0;
            caret = buffer.length();
        }

        /** Inserts {@code text} in place of the selection, dropping control characters and what does not fit. */
        void insert(String text) {
            if (hasSelection()) deleteSelection();
            for (int i = 0; i < text.length() && buffer.length() < maxLength; i++) {
                char c = text.charAt(i);
                if (c < 32 || c == 127) continue;
                buffer.insert(caret++, c);
            }
            anchor = caret;
        }

        /** Deletes the selection, or else the character before the caret, or the word before it with {@code word}. */
        void backspace(boolean word) {
            if (hasSelection()) {
                deleteSelection();
                return;
            }
            if (caret == 0) return;
            int from = word ? wordBefore(caret) : caret - 1;
            buffer.delete(from, caret);
            caret = from;
            anchor = from;
        }

        /** Deletes the selection, or else the character after the caret, or the word after it with {@code word}. */
        void delete(boolean word) {
            if (hasSelection()) {
                deleteSelection();
                return;
            }
            if (caret >= buffer.length()) return;
            buffer.delete(caret, word ? wordAfter(caret) : caret + 1);
        }

        /** Moves the caret; {@code extend} keeps the anchor where it is, selecting what lies between. */
        void moveCaret(int to, boolean extend) {
            caret = Math.max(0, Math.min(buffer.length(), to));
            if (! extend) anchor = caret;
        }

        /**
         * Moves one character, or one word with {@code word}, in {@code direction} (-1 or 1).
         * A plain arrow press with a selection collapses it to that side's edge instead.
         */
        void step(int direction, boolean word, boolean extend) {
            if (! extend && ! word && hasSelection()) {
                moveCaret(direction < 0 ? selectionStart() : selectionEnd(), false);
                return;
            }
            int to = word ? (direction < 0 ? wordBefore(caret) : wordAfter(caret)) : caret + direction;
            moveCaret(to, extend);
        }

        /** @return the start of the word before {@code from}, skipping the spaces in between */
        int wordBefore(int from) {
            int i = from;
            while (i > 0 && Character.isWhitespace(buffer.charAt(i - 1))) i--;
            while (i > 0 && ! Character.isWhitespace(buffer.charAt(i - 1))) i--;
            return i;
        }

        /** @return the end of the word after {@code from}, skipping the spaces in between */
        int wordAfter(int from) {
            int i = from;
            int length = buffer.length();
            while (i < length && Character.isWhitespace(buffer.charAt(i))) i++;
            while (i < length && ! Character.isWhitespace(buffer.charAt(i))) i++;
            return i;
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
        if (selected == null || (! BROWSE.equals(selected) && ! CLIENT.equals(selected)
                && modules.stream().noneMatch(m -> m.getId().equals(selected)))) {
            selected = modules.isEmpty() ? CLIENT : modules.get(0).getId();
            contentScroll = 0;
        }
        lastSelected = selected;
        if (available == null && availableError == null
                && (StreamlineClientSettings.checkECloud() || BROWSE.equals(selected))) {
            fetchAvailable();
        }

        drawSidebar(canvas, mouseX, mouseY, modules);
        drawContent(canvas, mouseX, mouseY, modules);
        drawDetails(canvas, mouseX, mouseY, modules);
    }

    /**
     * The details popup over the whole menu: it takes every click while open, and a click
     * outside it, its Close button or Escape closes it.
     */
    private void drawDetails(Canvas canvas, int mouseX, int mouseY, List<ClientModuleManager.Installed> modules) {
        if (detailsFor != null) {
            ClientModuleManager.Installed module = modules.stream().filter(m -> m.getId().equals(detailsFor)).findFirst().orElse(null);
            if (module == null) {
                closeDetails();
                return;
            }
            List<String> rows = new ArrayList<>();
            rows.add("&7Version: &f" + module.getVersion());
            if (! module.getProvider().isEmpty()) rows.add("&7Author: &f" + module.getProvider());
            rows.add("&7Identifier: &f" + module.getId());
            if (module.getFileName() != null && ! module.getFileName().isEmpty()) rows.add("&7File: &f" + module.getFileName());
            rows.add("&7Status: &f" + (module.isEnabled() ? "Enabled" : "Disabled"));
            drawPopup(canvas, mouseX, mouseY, displayName(module), rows, module.getDescription());
        } else if (listingDetailsFor != null) {
            List<ModuleRegistry.ModuleInfo> list = available;
            ModuleRegistry.ModuleInfo info = list == null ? null
                    : list.stream().filter(i -> listingKey(i).equals(listingDetailsFor)).findFirst().orElse(null);
            if (info == null) {
                closeDetails();
                return;
            }
            drawPopup(canvas, mouseX, mouseY, info.getName(), listingRows(info), info.getDescription());
        }
    }

    /** The details popup's rows for an eCloud entry: everything the registry says about it. */
    private static List<String> listingRows(ModuleRegistry.ModuleInfo info) {
        String key = listingKey(info);
        List<String> rows = new ArrayList<>();
        rows.add("&7Latest version: &f" + info.getVersion());
        String installed = ClientModuleCloud.installedVersion(key);
        rows.add("&7Installed: &f" + (installed == null ? "No" : installed));
        if (! info.getAuthor().isEmpty()) rows.add("&7Author: &f" + info.getAuthor());
        rows.add("&7Identifier: &f" + key);
        rows.add("&7Type: &f" + (ClientModuleCloud.isWorldModule(info)
                ? "World module (singleplayer or a LAN world you host)" : "Client module"));
        if (! info.getPlatforms().isEmpty()) rows.add("&7Platforms: &f" + String.join(", ", info.getPlatforms()));
        if (! info.getLicense().isEmpty()) rows.add("&7License: &f" + info.getLicense());
        if (! info.getRequires().isEmpty()) rows.add("&7Requires: &f" + info.getRequires());
        if (! info.getDependencies().isEmpty()) rows.add("&7Dependencies: &f" + String.join(", ", info.getDependencies()));
        rows.add("&7Downloads: &f" + info.getDownloads());
        if (info.getSize() > 0) rows.add("&7Size: &f" + formatSize(info.getSize()));
        if (! info.getUpdatedAt().isEmpty()) rows.add("&7Updated: &f" + info.getUpdatedAt());
        if (! info.getVersions().isEmpty()) {
            List<String> versions = new ArrayList<>();
            for (ModuleRegistry.VersionInfo version : info.getVersions()) versions.add(version.getVersion());
            rows.add("&7Versions: &f" + String.join(", ", versions));
        }
        return rows;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /** The key an eCloud entry is installed, updated and tracked under: its id, else its name. */
    private static String listingKey(ModuleRegistry.ModuleInfo info) {
        return info.getId().isEmpty() ? info.getName() : info.getId();
    }

    private boolean detailsOpen() {
        return detailsFor != null || listingDetailsFor != null;
    }

    private void closeDetails() {
        detailsFor = null;
        listingDetailsFor = null;
    }

    /**
     * Draws the details popup: {@code title}, then one {@code &}-coded line per row, then the
     * wrapped description, with a Close button at the bottom.
     */
    private void drawPopup(Canvas canvas, int mouseX, int mouseY, String title, List<String> rows, String descriptionText) {
        regions.clear();
        regions.add(new Region(0, 0, canvas.width(), canvas.height(), null, (x, y, b) -> closeDetails()));
        canvas.fill(0, 0, canvas.width(), canvas.height(), 0xA0000000);

        int width = Math.min(canvas.width() - 40, 300);
        int textWidth = width - PAD * 2;
        List<String> description = Theme.wrap(canvas, descriptionText, textWidth);

        int line = canvas.lineHeight() + 2;
        int height = PAD + line + 4 + rows.size() * line + (description.isEmpty() ? 0 : 6 + description.size() * line) + 8 + 14 + PAD;
        height = Math.min(height, canvas.height() - 20);
        int x1 = (canvas.width() - width) / 2;
        int y1 = (canvas.height() - height) / 2;
        int x2 = x1 + width;
        int y2 = y1 + height;
        regions.add(new Region(x1, y1, x2, y2, null, (x, y, b) -> { }));
        canvas.fill(x1, y1, x2, y2, Theme.PANEL);
        canvas.outline(x1, y1, x2, y2, Theme.CONTROL_BORDER);

        canvas.clip(x1, y1, x2, y2 - PAD - 18);
        int y = y1 + PAD;
        canvas.text(Theme.fit(canvas, "&l" + title, textWidth), x1 + PAD, y, Theme.TEXT, false);
        y += line + 4;
        for (String row : rows) {
            canvas.text(Theme.fit(canvas, row, textWidth), x1 + PAD, y, Theme.TEXT, false);
            y += line;
        }
        if (! description.isEmpty()) {
            y += 6;
            for (String text : description) {
                canvas.text(text, x1 + PAD, y, Theme.MUTED, false);
                y += line;
            }
        }
        canvas.endClip();

        int closeX2 = x2 - PAD;
        int closeX1 = closeX2 - 50;
        int closeY1 = y2 - PAD - 14;
        Theme.button(canvas, closeX1, closeY1, closeX2, closeY1 + 14, "Close",
                Theme.inside(mouseX, mouseY, closeX1, closeY1, closeX2, closeY1 + 14), true);
        regions.add(new Region(closeX1, closeY1, closeX2, closeY1 + 14, null, (x, yy, b) -> closeDetails()));
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
        int total = (modules.size() + 2) * rowHeight + 16;
        sidebarOverflow = Math.max(0, total - (sidebarY2 - sidebarY1));
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, sidebarOverflow));

        canvas.clip(sidebarX1, sidebarY1, sidebarX2, sidebarY2);
        int y = sidebarY1 + 4 - (int) sidebarScroll;
        sidebarRow(canvas, mouseX, mouseY, CLIENT, "Client Settings", Theme.ACCENT, y, rowHeight);
        y += rowHeight + 4;
        canvas.fill(sidebarX1 + 6, y - 2, sidebarX2 - 6, y - 1, Theme.CONTROL_BORDER);
        for (ClientModuleManager.Installed module : modules) {
            sidebarRow(canvas, mouseX, mouseY, module.getId(), displayName(module), 0, y, rowHeight);
            sidebarToggle(canvas, mouseX, mouseY, module, y, rowHeight);
            y += rowHeight;
        }
        y += 4;
        canvas.fill(sidebarX1 + 6, y - 2, sidebarX2 - 6, y - 1, Theme.CONTROL_BORDER);
        sidebarRow(canvas, mouseX, mouseY, BROWSE, "Browse eCloud", Theme.ACCENT_LIGHT, y, rowHeight);
        canvas.endClip();
    }

    /** The switch left of a module's name, which turns it on and off; drawn after its row so it takes the click. */
    private void sidebarToggle(Canvas canvas, int mouseX, int mouseY, ClientModuleManager.Installed module, int y, int rowHeight) {
        String id = module.getId();
        int x1 = sidebarX1 + 5;
        int ty = y + (rowHeight - 10) / 2;
        if (pending.contains(id)) {
            canvas.text("...", x1 + 4, y + 5, Theme.MUTED);
            return;
        }
        boolean visible = ty + 10 > sidebarY1 && ty < sidebarY2;
        boolean hover = visible && Theme.inside(mouseX, mouseY, x1, ty, x1 + 20, ty + 10);
        Theme.toggle(canvas, x1, ty, module.isEnabled(), hover);
        if (visible) {
            regions.add(new Region(x1, Math.max(ty, sidebarY1), x1 + 20, Math.min(ty + 10, sidebarY2), null,
                    (x, yy, b) -> toggleModule(id, module.isEnabled())));
        }
    }

    /** @param dot the marker's color, or {@code 0} for a module's row, whose left holds its switch */
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
        int labelX = dot == 0 ? sidebarX1 + 29 : sidebarX1 + 16;
        if (dot != 0) canvas.fill(sidebarX1 + 7, y + 7, sidebarX1 + 11, y + 11, dot);
        canvas.text(Theme.fit(canvas, label, sidebarX2 - labelX - 6), labelX, y + 5,
                isSelected ? Theme.TEXT : Theme.MUTED);
        if (visible) {
            regions.add(new Region(sidebarX1, Math.max(y, sidebarY1), sidebarX2, Math.min(y + rowHeight, sidebarY2), null, (x, yy, b) -> {
                if (! key.equals(selected)) {
                    commitEdit();
                    selected = key;
                    contentScroll = 0;
                    confirmRemove = null;
                    clearShortNotices();
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
        if (CLIENT.equals(selected)) y = drawClientSettings(canvas, mouseX, mouseY, y, textWidth);
        else if (module == null) y = drawBrowse(canvas, mouseX, mouseY, modules, y, textWidth);
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

    /**
     * A {@link #paragraph} where each {@link #WORLD_TAG_TOKEN} word is drawn as the world tag
     * pill, inline with the words around it. Lines holding a pill get two extra pixels above and
     * below, since the pill is taller than the text.
     */
    private int taggedParagraph(Canvas canvas, String text, int y, int width, int color) {
        int x1 = contentX1 + PAD;
        int space = canvas.textWidth(" ");
        List<List<String>> lines = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int used = 0;
        for (String word : text.split(" ")) {
            if (word.isEmpty()) continue;
            int wordWidth = WORLD_TAG_TOKEN.equals(word) ? Theme.tagWidth(canvas, WORLD_TAG) : canvas.textWidth(word);
            int needed = current.isEmpty() ? wordWidth : used + space + wordWidth;
            if (! current.isEmpty() && needed > width) {
                lines.add(current);
                current = new ArrayList<>();
                needed = wordWidth;
            }
            current.add(word);
            used = needed;
        }
        if (! current.isEmpty()) lines.add(current);

        for (List<String> line : lines) {
            boolean tagged = line.contains(WORLD_TAG_TOKEN);
            if (tagged) y += 2;
            int x = x1;
            for (String word : line) {
                if (WORLD_TAG_TOKEN.equals(word)) {
                    x = Theme.tag(canvas, x, y, WORLD_TAG);
                } else {
                    canvas.text(word, x, y, color, false);
                    x += canvas.textWidth(word);
                }
                x += space;
            }
            y += canvas.lineHeight() + 2 + (tagged ? 2 : 0);
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

        y = drawModuleActions(canvas, mouseX, mouseY, module, y, x1, right);
        y += 6;

        ModuleSettings page = ClientSettingsRegistry.get(id);
        if (! module.isEnabled()) {
            y = paragraph(canvas, "Turn the module on with its switch in the list to change its settings.", y, textWidth, Theme.MUTED);
        } else if (page != null && ! page.getSettings().isEmpty()) {
            y = section(canvas, "Settings", y, right);
            for (Setting<?> setting : page.getSettings()) {
                y = drawSetting(canvas, mouseX, mouseY, page, setting, y, x1, right);
            }
        }

        List<HudRegistry.Registered> elements = HudRegistry.elementsOf(id);
        CustomHudElements custom = module.isEnabled() ? HudRegistry.customElements(id) : null;
        if (! elements.isEmpty() || custom != null) {
            y += 4;
            int headerY = y;
            y = section(canvas, "HUD elements", y, right);
            if (custom != null) {
                boolean adding = id.equals(addingFor);
                int plusX1 = right - 14;
                Theme.button(canvas, plusX1, headerY - 3, right, headerY + 9, adding ? "-" : "+",
                        hovered(mouseX, mouseY, plusX1, headerY - 3, right, headerY + 9), true);
                addRegion(plusX1, headerY - 3, right, headerY + 9, null, (cx, cy, b) -> toggleAddForm(id));
                if (adding) y = drawAddForm(canvas, mouseX, mouseY, id, custom, y, x1, right);
            }
            for (HudRegistry.Registered registered : elements) {
                y = drawElement(canvas, mouseX, mouseY, registered, custom, y, x1, right);
            }
            y += 2;
            int buttonX2 = x1 + 100;
            boolean hover = hovered(mouseX, mouseY, x1, y, buttonX2, y + 14);
            Theme.button(canvas, x1, y, buttonX2, y + 14, "Edit HUD Layout", hover, true);
            addRegion(x1, y, buttonX2, y + 14, null, (cx, cy, b) -> openHudEditor());
            y += 18;
        }
        return y;
    }

    /** The Client Settings page: StreamlineClient's own settings, and the placeholders it fills. */
    private int drawClientSettings(Canvas canvas, int mouseX, int mouseY, int y, int textWidth) {
        int x1 = contentX1 + PAD;
        int right = contentX2 - PAD - 4;
        StreamlineClientSettings page = StreamlineClientSettings.page();
        canvas.text("&lClient Settings", x1, y, Theme.TEXT, false);
        y += canvas.lineHeight() + 4;
        y = paragraph(canvas, page.getDescription(), y, textWidth, Theme.MUTED);
        y += 6;
        y = section(canvas, "Settings", y, right);
        for (Setting<?> setting : page.getSettings()) {
            y = drawSetting(canvas, mouseX, mouseY, page, setting, y, x1, right);
        }
        y += 4;
        y = section(canvas, "Placeholders", y, right);
        y = paragraph(canvas, "Usable in HUD element text.", y, textWidth, Theme.MUTED);
        y += 2;
        for (Map.Entry<String, String> entry : ClientPlaceholders.descriptions().entrySet()) {
            String token = entry.getKey();
            canvas.text(Theme.fit(canvas, token, textWidth), x1, y, Theme.TEXT);
            y += canvas.lineHeight() + 1;
            int swatch = swatchColor(ClientPlaceholders.apply(token));
            int textX = x1 + 4;
            if (swatch != 0) {
                canvas.fill(textX, y, textX + 8, y + 8, swatch);
                textX += 12;
            }
            canvas.text(Theme.fit(canvas, entry.getValue(), right - textX), textX, y, Theme.MUTED);
            y += canvas.lineHeight() + 4;
        }
        return y;
    }

    /** @return the opaque color of a {@code {#rrggbb}} value, or {@code 0} for any other value */
    private static int swatchColor(String value) {
        if (value == null || value.length() != 9 || ! value.startsWith("{#") || value.charAt(8) != '}') return 0;
        try {
            return 0xFF000000 | Integer.parseInt(value.substring(2, 8), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
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
                int maxLength = setting instanceof Setting.Text ? ((Setting.Text) setting).getMaxLength() : 9;
                field(canvas, mouseX, mouseY, setting, fieldX1, y, cx2,
                        isColor ? setting.formatValue() : String.valueOf(setting.getValue()), false, maxLength, text -> {
                            setting.setFromText(text);
                            page.save();
                        });
                break;
            }
            case KEY: {
                Setting.Key key = (Setting.Key) setting;
                boolean listening = capturing == key;
                boolean hover = hovered(mouseX, mouseY, cx1, y - 1, cx2, y + 11);
                canvas.fill(cx1, y - 1, cx2, y + 11, hover ? Theme.CONTROL_BORDER : Theme.CONTROL);
                canvas.outline(cx1, y - 1, cx2, y + 11, listening ? Theme.ACCENT : Theme.CONTROL_BORDER);
                String label = listening ? "> Press a key <" : keyLabel(key);
                label = Theme.fit(canvas, label, controlWidth - 6);
                canvas.text(label, cx1 + (controlWidth - canvas.textWidth(label)) / 2, y + 1, listening ? Theme.ACCENT : Theme.TEXT);
                addRegion(cx1, y - 1, cx2, y + 11, setting, (x, yy, b) -> {
                    if (b == 1) {
                        capturing = null;
                        key.set(Setting.Key.NONE);
                        page.save();
                    } else if (listening) {
                        capturing = null;
                    } else {
                        capturing = key;
                        capturingPage = page;
                    }
                });
                break;
            }
            default:
                break;
        }
        return y + rowHeight;
    }

    private static String keyLabel(Setting.Key key) {
        if (! key.isBound()) return "None";
        String name = ClientCompat.keyDisplayName(key.getValue());
        return name == null || name.isEmpty() ? key.getValue() : name;
    }

    private static int controlWidth(Setting<?> setting) {
        switch (setting.getKind()) {
            case TOGGLE:
                return 20;
            case SLIDER:
                return 120;
            case CHOICE:
            case KEY:
                return 90;
            case COLOR:
                return 86;
            case TEXT:
            default:
                return 120;
        }
    }

    /**
     * Draws a one-line text field from {@code x1} to {@code x2}; clicking it starts typing into
     * it, and {@code apply} gets the text on Enter or when another control takes focus.
     *
     * @param owner identifies the field across frames
     * @param raw   whether to show color codes as typed instead of applying them
     */
    private void field(Canvas canvas, int mouseX, int mouseY, Object owner, int x1, int y, int x2,
                       String value, boolean raw, int maxLength, Consumer<String> apply) {
        boolean focused = editing != null && editing.owner == owner;
        boolean hover = hovered(mouseX, mouseY, x1, y - 1, x2, y + 11);
        canvas.fill(x1, y - 1, x2, y + 11, Theme.CONTROL);
        canvas.outline(x1, y - 1, x2, y + 11, focused ? Theme.ACCENT : (hover ? Theme.MUTED : Theme.CONTROL_BORDER));
        String shown = focused ? editing.buffer.toString() : (value == null ? "" : value);
        int caretAt = focused ? editing.caret : shown.length();
        int room = x2 - x1 - 8;
        int start = visibleStart(canvas, shown, caretAt, room, raw);
        String visibleText = fitHead(canvas, shown.substring(start), room, raw);
        if (focused && selectingText) {
            if (ClientCompat.leftMouseDown()) {
                editing.moveCaret(start + charsBefore(canvas, visibleText, mouseX - (x1 + 3), raw), true);
            } else {
                selectingText = false;
            }
        }
        if (focused && editing.hasSelection()) {
            int from = Math.max(start, editing.selectionStart());
            int to = Math.min(start + visibleText.length(), editing.selectionEnd());
            if (from < to) {
                int selX1 = x1 + 3 + width(canvas, shown.substring(start, from), raw);
                int selX2 = x1 + 3 + width(canvas, shown.substring(start, to), raw);
                canvas.fill(selX1, y, selX2, y + 10, Theme.SELECTION);
            }
        }
        if (raw) canvas.rawText(visibleText, x1 + 3, y + 1, Theme.TEXT);
        else canvas.text(visibleText, x1 + 3, y + 1, Theme.TEXT);
        boolean caret = focused && ((System.currentTimeMillis() - openedAt) / 500) % 2 == 0;
        if (caret) {
            int caretX = x1 + 3 + width(canvas, shown.substring(start, caretAt), raw);
            canvas.fill(caretX, y, caretX + 1, y + 10, Theme.TEXT);
        }
        addRegion(x1, y - 1, x2, y + 11, owner, (x, yy, b) -> {
            // Shift-clicking the focused field selects up to the click; clicking a field that had
            // no focus starts a fresh caret there.
            boolean extend = focused && ClientCompat.leftShiftDown();
            focus(owner, value, maxLength, apply);
            if (editing == null || editing.owner != owner) return;
            // A click inside the text puts the caret at the nearest character; holding the button
            // and moving selects from there (see selectingText).
            editing.moveCaret(start + charsBefore(canvas, visibleText, x - (x1 + 3), raw), extend);
            selectingText = b == 0;
        });
    }

    /** Starts typing into a field, keeping what was typed if it already has focus. */
    private void focus(Object owner, String value, int maxLength, Consumer<String> apply) {
        if (editing != null && editing.owner == owner) return;
        commitEdit();
        editing = new TextEdit(owner, value, maxLength, apply);
        openedAt = System.currentTimeMillis();
        textInput(true);
    }

    /** Leaves the focused field without applying what was typed. */
    private void dropEdit() {
        if (editing == null) return;
        editing = null;
        selectingText = false;
        textInput(false);
    }

    /** @return where the shown part of a field's text starts, so the caret stays in view */
    private static int visibleStart(Canvas canvas, String text, int caret, int width, boolean raw) {
        int start = 0;
        while (start < caret && width(canvas, text.substring(start, caret), raw) > width) start++;
        return start;
    }

    /** Cuts the text after as many characters as fit in {@code width}. */
    private static String fitHead(Canvas canvas, String text, int width, boolean raw) {
        int end = text.length();
        while (end > 0 && width(canvas, text.substring(0, end), raw) > width) end--;
        return text.substring(0, end);
    }

    /** @return how many characters of {@code text} lie left of {@code x}, rounding to the nearest gap */
    private static int charsBefore(Canvas canvas, String text, double x, boolean raw) {
        for (int i = 1; i <= text.length(); i++) {
            int left = width(canvas, text.substring(0, i - 1), raw);
            int right = width(canvas, text.substring(0, i), raw);
            if (x < (left + right) / 2.0) return i - 1;
        }
        return text.length();
    }

    private static int width(Canvas canvas, String text, boolean raw) {
        return raw ? canvas.rawTextWidth(text) : canvas.textWidth(text);
    }

    // ---------------------------------------------------------------- hud elements

    /** One HUD element: its name, an Edit button where its text can be edited, and its show switch. */
    private int drawElement(Canvas canvas, int mouseX, int mouseY, HudRegistry.Registered registered,
                            CustomHudElements custom, int y, int x1, int right) {
        HudElement element = registered.getElement();
        HudPlacement placement = registered.getPlacement();
        String key = registered.getKey();
        boolean editable = template(element) != null;
        boolean expanded = editable && key.equals(expandedElement);

        if (hovered(mouseX, mouseY, x1 - 2, y - 2, right + 2, y + 12)) canvas.fill(x1 - 2, y - 2, right + 2, y + 12, Theme.HOVER);
        int labelRight = right - 30;
        if (editable) {
            String label = expanded ? "Close" : "Edit";
            int editX2 = right - 26;
            int editX1 = editX2 - canvas.textWidth(label) - 10;
            Theme.button(canvas, editX1, y - 1, editX2, y + 11, label, hovered(mouseX, mouseY, editX1, y - 1, editX2, y + 11), false);
            addRegion(editX1, y - 1, editX2, y + 11, null, (cx, cy, b) -> {
                commitEdit();
                expandedElement = expanded ? null : key;
                confirmDelete = null;
            });
            labelRight = editX1 - 4;
        }
        if (editable) {
            // The name opens the editor too, not only the Edit button.
            addRegion(x1 - 2, y - 2, labelRight, y + 12, null, (cx, cy, b) -> {
                commitEdit();
                expandedElement = expanded ? null : key;
                confirmDelete = null;
            });
        }
        String name = element.getDisplayName() + (custom(element) ? " &8(custom)" : "");
        canvas.text(Theme.fit(canvas, name, labelRight - x1), x1, y + 1, Theme.TEXT);
        boolean hover = hovered(mouseX, mouseY, right - 20, y, right, y + 10);
        Theme.toggle(canvas, right - 20, y, placement.isEnabled(), hover);
        addRegion(right - 20, y, right, y + 10, null, (cx, cy, b) -> {
            // Showing also brings back an element removed in the HUD editor.
            if (placement.isEnabled()) placement.setEnabled(false);
            else placement.show();
            HudRegistry.save();
        });
        y += 16;
        if (expanded) y = drawTemplateEditor(canvas, mouseX, mouseY, registered, custom, y, x1, right);
        return y;
    }

    /** The open text editor of one element: the field, a preview, Reset / Delete, and its placeholders. */
    private int drawTemplateEditor(Canvas canvas, int mouseX, int mouseY, HudRegistry.Registered registered,
                                   CustomHudElements custom, int y, int x1, int right) {
        HudElement element = registered.getElement();
        String key = registered.getKey();
        int inner = x1 + 6;
        int top = y;
        Consumer<String> apply = text -> guard(key, () -> element.setTemplate(text));

        field(canvas, mouseX, mouseY, element, inner, y, right, template(element), true, TEMPLATE_MAX_LENGTH, apply);
        y += 16;

        y = drawPreview(canvas, registered, y, inner, right);

        int x = actionButton(canvas, mouseX, mouseY, inner, y, "Reset", false, () -> {
            if (editing != null && editing.owner == element) dropEdit();
            guard(key, () -> element.setTemplate(""));
        });
        if (custom != null && custom(element)) {
            boolean confirming = key.equals(confirmDelete);
            actionButton(canvas, mouseX, mouseY, x, y, confirming ? "Click again to delete" : "Delete", confirming, () -> {
                if (! key.equals(confirmDelete)) {
                    confirmDelete = key;
                    return;
                }
                confirmDelete = null;
                if (editing != null && editing.owner == element) dropEdit();
                String error = call(key, () -> custom.remove(element.getId()));
                if (error == null) {
                    expandedElement = null;
                    HudRegistry.forgetPlacement(registered.getOwner(), element.getId());
                    HudRegistry.save();
                }
            });
        }
        y = notice(canvas, key, y + 18, inner, right);

        Map<String, String> placeholders = placeholders(element);
        if (! placeholders.isEmpty()) {
            canvas.text("Placeholders (click to insert):", inner, y + 2, Theme.MUTED);
            y += canvas.lineHeight() + 5;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String token = entry.getKey();
                int tokenWidth = canvas.rawTextWidth(token);
                int rowY2 = y + canvas.lineHeight();
                boolean hover = hovered(mouseX, mouseY, inner - 2, y - 1, right + 2, rowY2);
                if (hover) canvas.fill(inner - 2, y - 1, right + 2, rowY2, Theme.HOVER);
                canvas.rawText(token, inner, y, hover ? Theme.ACCENT_LIGHT : Theme.TEXT);
                if (! entry.getValue().isEmpty()) {
                    canvas.text(Theme.fit(canvas, entry.getValue(), right - inner - tokenWidth - 8), inner + tokenWidth + 6, y, Theme.MUTED);
                }
                // Owned by the element's field, so clicking here keeps typing in it.
                addRegion(inner - 2, y - 1, right + 2, rowY2, element, (cx, cy, b) -> {
                    focus(element, template(element), TEMPLATE_MAX_LENGTH, apply);
                    editing.insert(token);
                });
                y += canvas.lineHeight() + 2;
            }
        }
        canvas.fill(x1, top - 2, x1 + 2, y, Theme.CONTROL_BORDER);
        return y + 6;
    }

    /** Draws the element as the HUD would, scaled down if it is wider than the menu. */
    private int drawPreview(Canvas canvas, HudRegistry.Registered registered, int y, int x1, int right) {
        HudElement element = registered.getElement();
        try {
            CanvasHudRenderer renderer = new CanvasHudRenderer(canvas);
            int width = element.width(renderer, true);
            int height = element.height(renderer, true);
            if (width <= 0 || height <= 0) return y;
            float scale = Math.min(1f, (right - x1) / (float) width);
            canvas.push();
            try {
                canvas.translate(x1, y);
                canvas.scale(scale);
                element.render(renderer, true);
            } finally {
                canvas.pop();
            }
            return y + (int) Math.ceil(height * scale) + 4;
        } catch (Throwable t) {
            return paragraph(canvas, "Preview failed: " + t, y, right - x1, Theme.ACCENT_LIGHT);
        }
    }

    /** The form the + button opens: an identifier, the text, and Add / Cancel. */
    private int drawAddForm(Canvas canvas, int mouseX, int mouseY, String moduleId, CustomHudElements custom,
                            int y, int x1, int right) {
        int labelWidth = Math.max(canvas.textWidth("Identifier"), canvas.textWidth("Text")) + 8;
        canvas.text("Identifier", x1, y + 1, Theme.TEXT);
        field(canvas, mouseX, mouseY, addIdField, x1 + labelWidth, y, Math.min(right, x1 + labelWidth + 120), addId, true,
                ID_MAX_LENGTH, text -> addId = text);
        y += 16;
        canvas.text("Text", x1, y + 1, Theme.TEXT);
        field(canvas, mouseX, mouseY, addTextField, x1 + labelWidth, y, right, addText, true,
                TEMPLATE_MAX_LENGTH, text -> addText = text);
        y += 16;
        y = paragraph(canvas, "Color codes and placeholders work here; Edit lists the placeholders once it is added. "
                + "It starts at the top left: move it with HUD Layout.", y, right - x1, Theme.MUTED);
        String noticeKey = "add:" + moduleId;
        int x = actionButton(canvas, mouseX, mouseY, x1, y, "Add", true, () -> {
            String id = addId.trim();
            String error = call(noticeKey, () -> custom.add(id, addText));
            if (error == null) {
                notices.put(noticeKey, "Added '" + id + "'.");
                expandedElement = HudRegistry.key(moduleId, id);
                addId = "";
                addText = "";
            }
        });
        actionButton(canvas, mouseX, mouseY, x, y, "Cancel", false, () -> toggleAddForm(moduleId));
        return notice(canvas, noticeKey, y + 18, x1, right) + 4;
    }

    private void toggleAddForm(String moduleId) {
        commitEdit();
        if (moduleId.equals(addingFor)) {
            addingFor = null;
        } else {
            addingFor = moduleId;
            addId = "";
            addText = "";
        }
        notices.remove("add:" + moduleId);
    }

    // Element methods are module code, so each call is caught: a module that throws only
    // fails its own element.

    private static String template(HudElement element) {
        try {
            return element.getTemplate();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Map<String, String> placeholders(HudElement element) {
        try {
            Map<String, String> placeholders = element.getPlaceholders();
            return placeholders == null ? Collections.<String, String>emptyMap() : placeholders;
        } catch (Throwable t) {
            return Collections.emptyMap();
        }
    }

    private static boolean custom(HudElement element) {
        try {
            return element.isCustom();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Runs module code for the element or form at {@code key}, noting a failure under it. */
    private void guard(String key, Runnable action) {
        try {
            action.run();
            notices.remove(key);
        } catch (Throwable t) {
            notices.put(key, "!" + message(t));
            singularity.client.ClientEnvironment.logWarning("Menu action '" + key + "' failed", t);
        }
    }

    /** Asks module code for an outcome; {@code null} is success, anything else is noted as a failure. */
    private String call(String key, Callable<String> action) {
        String error;
        try {
            error = action.call();
        } catch (Throwable t) {
            error = message(t);
            singularity.client.ClientEnvironment.logWarning("Menu action '" + key + "' failed", t);
        }
        if (error == null) notices.remove(key);
        else notices.put(key, "!" + error);
        return error;
    }

    private static String formatNumber(Double value, double step) {
        if (value == null) return "";
        if (step >= 1 && Math.rint(step) == step) return String.valueOf(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // ---------------------------------------------------------------- actions

    /**
     * The module's one-line header, {@code <name> <version> by <author>}, with Details, Reload,
     * Update (when eCloud lists a newer version, or another jar of this one) and Remove beside it, or below it when the line
     * is too narrow; then the last action's outcome.
     */
    private int drawModuleActions(Canvas canvas, int mouseX, int mouseY, ClientModuleManager.Installed module,
                                  int y, int x1, int right) {
        String id = module.getId();
        String title = "&l" + displayName(module) + "&r &7" + module.getVersion()
                + (module.getProvider().isEmpty() ? "" : " by " + module.getProvider());

        boolean busy = pending.contains(id);
        ModuleRegistry.ModuleInfo listed = listed(id);
        String update = listed != null && ModuleRegistry.updateAvailable(module.getVersion(), module.getJar(), listed)
                ? "Update to " + listed.getVersion() : null;
        boolean confirming = id.equals(confirmRemove);
        String remove = confirming ? "Click again to remove" : "Remove";
        List<String> labels = new ArrayList<>();
        labels.add("Details");
        if (! busy) {
            labels.add("Reload");
            if (update != null) labels.add(update);
            labels.add(remove);
        }
        int buttons = -6;
        for (String label : labels) buttons += canvas.textWidth(label) + 14 + 6;
        if (busy) buttons += 6 + canvas.textWidth("Working...");

        int x;
        if (x1 + canvas.textWidth(title) + 10 + buttons <= right) {
            canvas.text(title, x1, y + 3, Theme.TEXT, false);
            x = right - buttons;
        } else {
            canvas.text(Theme.fit(canvas, title, right - x1), x1, y, Theme.TEXT, false);
            y += canvas.lineHeight() + 4;
            x = x1;
        }

        x = actionButton(canvas, mouseX, mouseY, x, y, "Details", false, () -> detailsFor = id);
        if (busy) {
            canvas.text("Working...", x, y + 3, Theme.MUTED);
        } else {
            x = actionButton(canvas, mouseX, mouseY, x, y, "Reload", false,
                    () -> run(id, ClientModuleManager.reload(id), m -> "Reloaded " + m.getVersion() + "."));
            if (update != null) {
                x = actionButton(canvas, mouseX, mouseY, x, y, update, true,
                        () -> run(id, ClientModuleCloud.update(id), result -> "Updated!", UPDATED_NOTICE_MILLIS));
            }
            actionButton(canvas, mouseX, mouseY, x, y, remove, confirming, () -> {
                if (! id.equals(confirmRemove)) {
                    confirmRemove = id;
                    return;
                }
                confirmRemove = null;
                run(id, ClientModuleManager.remove(id), jar -> "Removed " + jar + ".");
            });
        }
        return notice(canvas, id, y + 18, x1, right);
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
        Long expiry = noticeExpiry.get(key);
        if (expiry != null && System.currentTimeMillis() >= expiry) {
            noticeExpiry.remove(key);
            notices.remove(key);
        }
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
        run(key, action, success, 0L);
    }

    /**
     * Like {@link #run(String, CompletableFuture, Function)}, but a success notice disappears after
     * {@code successMillis} or when the menu switches pages ({@code 0} keeps it). Failures always stay.
     */
    private <T> void run(String key, CompletableFuture<T> action, Function<T, String> success, long successMillis) {
        if (! pending.add(key)) return;
        notices.remove(key);
        noticeExpiry.remove(key);
        action.whenComplete((value, error) -> {
            if (error == null) {
                if (successMillis > 0) noticeExpiry.put(key, System.currentTimeMillis() + successMillis);
                notices.put(key, success.apply(value));
            } else {
                notices.put(key, "!" + message(error));
            }
            pending.remove(key);
        });
    }

    /** Drops every short-lived notice, so none follows the player to another page. */
    private void clearShortNotices() {
        for (String key : noticeExpiry.keySet()) {
            noticeExpiry.remove(key);
            notices.remove(key);
        }
    }

    private void toggleModule(String id, boolean enabled) {
        if (enabled) run(id, ClientModuleManager.disable(id), m -> "Disabled. It stays off until you enable it.");
        else run(id, ClientModuleManager.enable(id), m -> "Enabled.");
    }

    /** The eCloud page: every client and world module the registry lists, with install and update buttons. */
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
        // eCloud offers world modules only where they can run, so the tag is explained only there.
        y = taggedParagraph(canvas, "You can install client modules straight from the eCloud below!"
                + (WorldModules.isSupported() ? " Modules tagged with " + WORLD_TAG_TOKEN
                        + " only work in Singleplayer or if you are hosting a LAN world." : ""),
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
        if (list.isEmpty()) return paragraph(canvas, "eCloud lists no modules for clients yet.", y, textWidth, Theme.MUTED);

        for (ModuleRegistry.ModuleInfo info : list) {
            y = drawListing(canvas, mouseX, mouseY, info, y, x1, right);
        }
        return y;
    }

    private int drawListing(Canvas canvas, int mouseX, int mouseY, ModuleRegistry.ModuleInfo info, int y, int x1, int right) {
        int top = y;
        String key = info.getId().isEmpty() ? info.getName() : info.getId();
        String installed = ClientModuleCloud.installedVersion(key);
        boolean outdated = installed != null && ClientModuleCloud.updateAvailable(info);
        boolean world = ClientModuleCloud.isWorldModule(info);

        String label;
        Runnable action = null;
        if (pending.contains(key)) {
            label = "Working...";
        } else if (installed == null) {
            label = "Install";
            action = () -> run(key, ClientModuleCloud.download(key, null), result -> "Installed " + result.getVersion()
                    + (result.isWorld() && ! result.isEnabled() ? ". It starts with the next world you open." : "."));
        } else if (outdated) {
            label = "Update";
            action = () -> run(key, ClientModuleCloud.update(key), result -> "Updated!"
                    + (result.isWorld() && ! result.isEnabled() ? " It starts with the next world you open." : ""),
                    UPDATED_NOTICE_MILLIS);
        } else {
            label = "Installed";
        }
        int buttonX1 = right - Math.max(56, canvas.textWidth(label) + 14);
        int detailsX2 = buttonX1 - 6;
        int detailsX1 = detailsX2 - canvas.textWidth("Details") - 14;
        int textWidth = detailsX1 - x1 - 8;

        if (hovered(mouseX, mouseY, x1 - 2, top - 3, right + 2, top + canvas.lineHeight() * 2 + 6)) {
            canvas.fill(x1 - 2, top - 3, right + 2, top + canvas.lineHeight() * 2 + 6, Theme.HOVER);
        }
        // <name> v<version> by <author>, then the entry's tags.
        int tagsWidth = world ? 4 + Theme.tagWidth(canvas, WORLD_TAG) : 0;
        String title = Theme.fit(canvas, "&l" + info.getName() + " &r&7v" + info.getVersion()
                + (info.getAuthor().isEmpty() ? "" : " by " + info.getAuthor()), Math.max(0, textWidth - tagsWidth));
        canvas.text(title, x1, y + 1, Theme.TEXT);
        if (world) Theme.tag(canvas, x1 + canvas.textWidth(title) + 4, y + 1, WORLD_TAG);

        Theme.button(canvas, detailsX1, y - 2, detailsX2, y + 12, "Details", hovered(mouseX, mouseY, detailsX1, y - 2, detailsX2, y + 12), false);
        addRegion(detailsX1, y - 2, detailsX2, y + 12, null, (cx, cy, b) -> listingDetailsFor = key);
        if (action != null) {
            Runnable click = action;
            Theme.button(canvas, buttonX1, y - 2, right, y + 12, label, hovered(mouseX, mouseY, buttonX1, y - 2, right, y + 12), true);
            addRegion(buttonX1, y - 2, right, y + 12, null, (cx, cy, b) -> click.run());
        } else {
            canvas.text(label, right - canvas.textWidth(label), y + 1, Theme.MUTED);
        }
        y += canvas.lineHeight() + 3;

        String byline = info.getDownloads() + (info.getDownloads() == 1 ? " download" : " downloads")
                + (installed != null ? "  (installed " + installed + ")" : "");
        canvas.text(Theme.fit(canvas, byline, right - x1), x1, y, Theme.MUTED);
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
        dropEdit();
        edit.apply.accept(edit.buffer.toString());
    }

    // ---------------------------------------------------------------- input

    @Override
    protected boolean mouseDown(double x, double y, int button) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region region = regions.get(i);
            if (! Theme.inside(x, y, region.x1, region.y1, region.x2, region.y2)) continue;
            if (editing != null && region.owner != editing.owner) commitEdit();
            if (capturing != null && region.owner != capturing) capturing = null;
            region.click.click(x, y, button);
            return true;
        }
        commitEdit();
        capturing = null;
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
        if (button == 0) selectingText = false;
        if (sliderDrag == null) return false;
        sliderDrag.page.save();
        sliderDrag = null;
        return true;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        if (detailsOpen()) return true;
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

    /** Binds the key the player pressed to the setting waiting for one; Escape leaves it as it was. */
    @Override
    protected boolean keyNamed(String name) {
        if (capturing == null) return false;
        Setting.Key key = capturing;
        capturing = null;
        if (! "key.keyboard.escape".equals(name)) {
            key.set(name);
            if (capturingPage != null) capturingPage.save();
        }
        return true;
    }

    @Override
    protected boolean key(int key, int modifiers) {
        if (detailsOpen() && key == Theme.GLFW_ESCAPE) {
            closeDetails();
            return true;
        }
        if (editing == null) return false;
        boolean shortcut = (modifiers & Theme.GLFW_MOD_SHORTCUT) != 0;
        boolean shift = (modifiers & Theme.GLFW_MOD_SHIFT) != 0;
        switch (key) {
            case Theme.GLFW_BACKSPACE:
                editing.backspace(shortcut);
                return true;
            case Theme.GLFW_DELETE:
                editing.delete(shortcut);
                return true;
            case Theme.GLFW_LEFT:
                editing.step(-1, shortcut, shift);
                return true;
            case Theme.GLFW_RIGHT:
                editing.step(1, shortcut, shift);
                return true;
            case Theme.GLFW_HOME:
                editing.moveCaret(0, shift);
                return true;
            case Theme.GLFW_END:
                editing.moveCaret(editing.buffer.length(), shift);
                return true;
            case Theme.GLFW_A:
                if (shortcut) editing.selectAll();
                return true;
            case Theme.GLFW_C:
                if (shortcut && editing.hasSelection()) setClipboard(editing.selected());
                return true;
            case Theme.GLFW_X:
                if (shortcut && editing.hasSelection()) {
                    setClipboard(editing.selected());
                    editing.deleteSelection();
                }
                return true;
            case Theme.GLFW_V:
                if (shortcut) editing.insert(clipboard());
                return true;
            // Escape leaves the field, keeping what was typed; the next Escape closes the menu.
            case Theme.GLFW_ENTER:
            case Theme.GLFW_KP_ENTER:
            case Theme.GLFW_ESCAPE:
                commitEdit();
                return true;
            default:
                // Swallow other keys so typing does not trigger keybinds behind the menu.
                return true;
        }
    }

    @Override
    protected boolean typed(String text) {
        if (editing == null || text == null) return false;
        editing.insert(text);
        return true;
    }

    @Override
    public void onClose() {
        commitEdit();
        ClientCompat.setScreen(parent);
    }
}
