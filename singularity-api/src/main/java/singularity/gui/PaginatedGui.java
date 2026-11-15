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
 *
 * <p>Like BukkitOfUtils' paginated menus, every draw starts from a shell
 * ({@link GuiLayout#fillShell}): a black border with corners in {@link #getCornerColor()}, or
 * the colour of the title when that is {@code null}. {@link #setShelled(boolean) setShelled(false)}
 * starts from an empty screen instead. The page buttons sit either side of the bottom-centre
 * back slot.</p>
 */
@Getter @Setter
public class PaginatedGui extends CosmicGui {
    private List<GuiIcon> entries;
    private List<Integer> contentSlots;
    private int page;
    private int previousSlot;
    private int nextSlot;
    /** The previous-page button; {@code null} uses {@link GuiItems#pagePreviousButton(int)}. */
    private CosmicItem previousItem;
    /** The next-page button; {@code null} uses {@link GuiItems#pageNextButton(int)}. */
    private CosmicItem nextItem;
    private boolean shelled;
    /** The shell's corner colour; {@code null} matches the title's first colour. */
    private CornerColor cornerColor;

    public PaginatedGui(String title, int rows, List<Integer> contentSlots) {
        super(title, rows);
        this.entries = new ArrayList<>();
        this.contentSlots = new ArrayList<>(contentSlots);
        this.page = 1;
        int back = GuiLayout.defaultBackSlot(getRows());
        this.previousSlot = GuiLayout.pagePrevSlot(back);
        this.nextSlot = GuiLayout.pageNextSlot(back);
        this.previousItem = null;
        this.nextItem = null;
        this.shelled = true;
        this.cornerColor = null;
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

    /**
     * Lays out everything other than the entries and page buttons. Content slots left empty by
     * the current page keep whatever this puts there.
     */
    protected void drawFrame(CosmicPlayer viewer) {
    }

    @Override
    public void draw(CosmicPlayer viewer) {
        clear();
        entries = buildEntries(viewer);
        if (entries == null) entries = new ArrayList<>();
        if (page > getMaxPages()) page = getMaxPages();

        if (shelled) shell(cornerColor == null ? CornerColor.fromDisplayText(getTitle()) : cornerColor);
        drawFrame(viewer);

        int start = (page - 1) * getPerPage();
        for (int i = 0; i < contentSlots.size(); i++) {
            int index = start + i;
            if (index < entries.size()) setIcon(contentSlots.get(i), entries.get(index));
        }

        if (hasPreviousPage()) {
            CosmicItem item = previousItem == null ? GuiItems.pagePreviousButton(page - 1)
                    : previousItem.copy().setLore("&7Page " + (page - 1));
            setIcon(previousSlot, item, click -> previousPage());
        }
        if (hasNextPage()) {
            CosmicItem item = nextItem == null ? GuiItems.pageNextButton(page + 1)
                    : nextItem.copy().setLore("&7Page " + (page + 1));
            setIcon(nextSlot, item, click -> nextPage());
        }
    }
}
