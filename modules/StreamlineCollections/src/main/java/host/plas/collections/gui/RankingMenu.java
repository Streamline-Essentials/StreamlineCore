package host.plas.collections.gui;

import host.plas.collections.data.Boards;
import host.plas.collections.database.TrackKeeper;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One leaderboard, paged: a head per ranked player, the viewer's own rank at the top and a back
 * button. {@code value} turns a row into its lore line.
 */
public class RankingMenu extends PaginatedGui {
    private final String accent;
    private final List<TrackKeeper.Ranked> rows;
    private final Function<TrackKeeper.Ranked, String> value;
    private final Consumer<CosmicPlayer> back;
    private final Consumer<CosmicPlayer> ownRankClick;

    /**
     * @param ownRankClick what clicking the viewer's own rank does, or {@code null} for nothing
     */
    public RankingMenu(String title, String accent, List<TrackKeeper.Ranked> rows, Function<TrackKeeper.Ranked, String> value,
                       Consumer<CosmicPlayer> back, Consumer<CosmicPlayer> ownRankClick) {
        super(title, Menus.ROWS, Menus.contentSlots());
        this.accent = accent;
        this.rows = rows == null ? new ArrayList<>() : rows;
        this.value = value;
        this.back = back;
        this.ownRankClick = ownRankClick;
    }

    @Override
    protected void drawFrame(CosmicPlayer viewer) {
        Menus.shell(this, accent);
        if (rows.isEmpty()) setItem(22, CosmicItem.of("minecraft:barrier", "#FF5555Nobody here yet"));

        int rank = Boards.rankOf(rows, viewer.getUuid());
        List<String> lore = new ArrayList<>();
        lore.add(rank > 0 ? "#FFFFFF#" + rank : "#FFFFFFNot in the top " + Boards.BOARD_SIZE);
        if (ownRankClick != null) {
            lore.add("");
            lore.add("#FFFFFFClick to view own stats");
        }
        CosmicItem head = CosmicItem.of("minecraft:player_head", "#00FC88ʏᴏᴜʀ ʀᴀɴᴋ").setLore(lore).setSkullOwner(viewer.getUuid());
        if (ownRankClick == null) setItem(4, head);
        else setIcon(4, head, click -> ownRankClick.accept(click.getViewer()));

        setIcon(Menus.BACK_SLOT, Menus.back(() -> back.accept(viewer)));
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        List<GuiIcon> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TrackKeeper.Ranked row = rows.get(i);
            CosmicItem head = CosmicItem.of("minecraft:player_head",
                    "#FFD700#" + (i + 1) + " #FFFFFF" + (row.name == null ? "?" : row.name),
                    value.apply(row)).setSkullOwner(row.uuid);
            entries.add(GuiIcon.of(head));
        }
        return entries;
    }
}
