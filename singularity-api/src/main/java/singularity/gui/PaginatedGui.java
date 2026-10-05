package singularity.gui;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link CosmicGui} that spreads a list of entries over pages, with previous/next buttons.
 *
 * <p>Entries fill {@link #getContentSlots() the content slots} in order. Subclasses lay out the
 * rest of the screen by overriding {@link #drawFrame(CosmicPlayer)} and supply the entries
 * through {@link #setEntries(List)} or by overriding {@link #buildEntries(CosmicPlayer)}, which
 * runs on every draw.</p>
 */
@Getter @Setter
public class PaginatedGui extends CosmicGui {
    private List<GuiIcon> entries;
    private List<Integer> contentSlots;
    private int page;
    private int previousSlot;
    private int nextSlot;
    private CosmicItem previousItem;
    private CosmicItem nextItem;

    public PaginatedGui(String title, int rows, List<Integer> contentSlots) {
        super(title, rows);
        this.entries = new ArrayList<>();
        this.contentSlots = new ArrayList<>(contentSlots);
        this.page = 1;
        int bottom = GuiLayout.bottomCenter(getRows());
        this.previousSlot = bottom - 1;
        this.nextSlot = bottom + 1;
        this.previousItem = CosmicItem.of("minecraft:arrow", "&bPrevious Page");
        this.nextItem = CosmicItem.of("minecraft:arrow", "&bNext Page");
    }

    /** A paginated GUI whose entries fill the slots inside the border. */
    public PaginatedGui(String title, int rows) {
        this(title, rows, GuiLayout.innerSlots(rows));
    }

    public int getPerPage() {
        return Math.max(1, contentSlots.size());
    }

    public int getMaxPages() {
        return Math.max(1, (int) Math.ceil(entries.size() / (double) getPerPage()));
    }

    public boolean hasPreviousPage() {
        return page > 1;
    }

    public boolean hasNextPage() {
        return page < getMaxPages();
    }

    public void setPage(int page) {
        this.page = Math.max(1, page);
    }

    public void nextPage() {
        if (! hasNextPage()) return;
        page++;
        redraw();
    }

    public void previousPage() {
        if (! hasPreviousPage()) return;
        page--;
        redraw();
    }

    /** Supplies the entries for this draw; the default keeps the current list. */
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        return entries;
    }

    /** Lays out everything other than the entries and page buttons. */
    protected void drawFrame(CosmicPlayer viewer) {
    }

    @Override
    public void draw(CosmicPlayer viewer) {
        clear();
        entries = buildEntries(viewer);
        if (entries == null) entries = new ArrayList<>();
        if (page > getMaxPages()) page = getMaxPages();

        drawFrame(viewer);

        int start = (page - 1) * getPerPage();
        for (int i = 0; i < contentSlots.size(); i++) {
            int index = start + i;
            if (index < entries.size()) setIcon(contentSlots.get(i), entries.get(index));
            else removeIcon(contentSlots.get(i));
        }

        if (hasPreviousPage()) {
            setIcon(previousSlot, previousItem.copy().setLore("&7Page " + (page - 1)), click -> previousPage());
        }
        if (hasNextPage()) {
            setIcon(nextSlot, nextItem.copy().setLore("&7Page " + (page + 1)), click -> nextPage());
        }
    }
}
