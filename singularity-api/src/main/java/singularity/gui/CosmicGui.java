package singularity.gui;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * A chest-style menu built once and shown on any platform: Spigot, the mod loaders, or a proxy,
 * which hands it to the player's backend server to render.
 *
 * <p>A GUI is a title, a row count and a map of slot to {@link GuiIcon}. It can be filled up
 * front, or a subclass can override {@link #draw(CosmicPlayer)} to lay itself out each time it
 * is opened or {@linkplain #redraw() redrawn} — the same split as BukkitOfUtils'
 * {@code InventorySheet} and {@code ScreenInstance}.</p>
 *
 * <p>Each instance has an id. Renderers echo it back with clicks and closes so that a click
 * meant for a GUI the player already left is ignored.</p>
 */
@Getter @Setter
public class CosmicGui {
    public static final int MAX_ROWS = 6;

    private final String id;
    private String title;
    private int rows;
    private ConcurrentSkipListMap<Integer, GuiIcon> icons;
    /** Runs on the building server when the viewer closes this GUI themselves. */
    private Consumer<CosmicPlayer> onClose;

    public CosmicGui(String title, int rows) {
        this.id = UUID.randomUUID().toString();
        this.title = title == null ? "" : title;
        this.rows = clampRows(rows);
        this.icons = new ConcurrentSkipListMap<>();
        this.onClose = null;
    }

    public static int clampRows(int rows) {
        return Math.max(1, Math.min(MAX_ROWS, rows));
    }

    public int getSize() {
        return rows * 9;
    }

    public void setRows(int rows) {
        this.rows = clampRows(rows);
        icons.keySet().removeIf(slot -> slot >= getSize());
    }

    /**
     * Lays this GUI out for {@code viewer}. Called before every open and redraw; the default
     * keeps whatever icons were set directly.
     */
    public void draw(CosmicPlayer viewer) {
    }

    public CosmicGui setIcon(int slot, GuiIcon icon) {
        if (slot < 0 || slot >= getSize()) return this;
        if (icon == null) icons.remove(slot);
        else icons.put(slot, icon);
        return this;
    }

    public CosmicGui setIcon(int slot, CosmicItem item, Consumer<GuiClick> onClick) {
        return setIcon(slot, new GuiIcon(item, onClick));
    }

    public CosmicGui setItem(int slot, CosmicItem item) {
        return setIcon(slot, new GuiIcon(item));
    }

    public CosmicGui removeIcon(int slot) {
        icons.remove(slot);
        return this;
    }

    public Optional<GuiIcon> getIcon(int slot) {
        return Optional.ofNullable(icons.get(slot));
    }

    public CosmicGui clear() {
        icons.clear();
        return this;
    }

    /** Puts {@code item} in every slot. */
    public CosmicGui fill(CosmicItem item) {
        for (int i = 0; i < getSize(); i++) setItem(i, item);
        return this;
    }

    /** Puts {@code item} in every slot that has no icon yet. */
    public CosmicGui fillEmpty(CosmicItem item) {
        for (int i = 0; i < getSize(); i++) {
            if (! icons.containsKey(i)) setItem(i, item);
        }
        return this;
    }

    /** Puts {@code item} in every slot of the outer ring. */
    public CosmicGui border(CosmicItem item) {
        for (int slot : GuiLayout.borderSlots(rows)) setItem(slot, item);
        return this;
    }

    /** Opens this GUI for {@code viewer}, replacing whatever GUI they had open. */
    public void open(CosmicPlayer viewer) {
        GuiManager.open(viewer, this);
    }

    /** Re-lays this GUI out and resends it to everyone currently viewing it. */
    public void redraw() {
        GuiManager.redraw(this);
    }

    /** Closes this GUI for everyone currently viewing it. */
    public void closeAll() {
        GuiManager.getViewers(this).forEach(GuiManager::close);
    }

    /**
     * The render-only snapshot of this GUI as it is now: items and which slots react to clicks,
     * without the handlers themselves.
     */
    public GuiView snapshot() {
        GuiView view = new GuiView(id, title, rows);
        for (Map.Entry<Integer, GuiIcon> entry : icons.entrySet()) {
            GuiIcon icon = entry.getValue();
            if (icon == null) continue;
            view.getItems().put(entry.getKey(), icon.getItem());
            if (icon.isClickable()) view.getClickable().add(entry.getKey());
        }
        return view;
    }
}
