package host.plas.collections.gui;

import host.plas.collections.data.CollectionCategory;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.database.CollectionKeeper;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /collectionsleaderboard}: a hub, a category's boards, and the rankings themselves.
 */
public final class LeaderboardMenus {
    private LeaderboardMenus() {
    }

    private static final String ACCENT = "#FFED6A";

    public static void openHub(CosmicPlayer viewer) {
        new Hub().open(viewer);
    }

    public static void openCategory(CosmicPlayer viewer, CollectionCategory category) {
        new CategoryBoards(category).open(viewer);
    }

    public static void openOverall(CosmicPlayer viewer) {
        openRanking(viewer, CollectionManager.overallBoard(), ACCENT + "&lOverall Collections", ACCENT, null);
    }

    public static void openCategoryTotal(CosmicPlayer viewer, CollectionCategory category) {
        openRanking(viewer, CollectionManager.boardFor(category),
                category.getColor() + "&l" + category.getName() + " Total", category.getColor(), category);
    }

    public static void openCollection(CosmicPlayer viewer, CollectionDefinition definition) {
        CollectionCategory category = definition.getCategory();
        openRanking(viewer, CollectionManager.boardFor(definition),
                category.getColor() + "&l" + definition.getDisplayName() + " LB", category.getColor(), category);
    }

    private static void openRanking(CosmicPlayer viewer, CompletableFuture<List<CollectionKeeper.Ranked>> board,
                                    String title, String accent, CollectionCategory backTo) {
        board.whenComplete((rows, error) ->
                new Ranking(title, accent, rows == null ? new ArrayList<>() : rows, backTo).open(viewer));
    }

    private static final class Hub extends CosmicGui {
        private Hub() {
            super(ACCENT + "&lCollections Leaderboards", Menus.ROWS);
        }

        @Override
        public void draw(CosmicPlayer viewer) {
            clear();
            Menus.shell(this, ACCENT);

            setIcon(13, CosmicItem.of("minecraft:nether_star", ACCENT + "&lOverall",
                    "#AAAAAATotal items collected across every item collection.", "",
                    "#bdc8c9Click: #bbff6aView leaderboard"), click -> openOverall(click.getViewer()));

            List<CollectionCategory> categories = new ArrayList<>(CollectionManager.getCatalog().getCategories().values());
            int[] slots = Menus.centredRow(2, categories.size());
            for (int i = 0; i < slots.length; i++) {
                CollectionCategory category = categories.get(i);
                setIcon(slots[i], CosmicItem.of(category.getIcon(), category.getColor() + "&l" + category.getName(),
                        "#AAAAAABrowse this category's leaderboards.", "",
                        "#bdc8c9Click: #bbff6aOpen"), click -> openCategory(click.getViewer(), category));
            }

            setIcon(Menus.BACK_SLOT, Menus.close());
        }
    }

    private static final class CategoryBoards extends PaginatedGui {
        private final CollectionCategory category;

        private CategoryBoards(CollectionCategory category) {
            super(category.getColor() + "&l" + category.getName() + " Leaderboards", Menus.ROWS, Menus.contentSlots());
            this.category = category;
        }

        @Override
        protected void drawFrame(CosmicPlayer viewer) {
            Menus.shell(this, category.getColor());
            if (category.isSummable()) {
                setIcon(4, CosmicItem.of("minecraft:gold_ingot", category.getColor() + "&lAll " + category.getName(),
                        "#AAAAAACombined totals for this category.", "",
                        "#bdc8c9Click: #bbff6aView leaderboard"), click -> openCategoryTotal(click.getViewer(), category));
            }
            setIcon(Menus.BACK_SLOT, Menus.back(() -> openHub(viewer)));
        }

        @Override
        protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
            List<GuiIcon> entries = new ArrayList<>();
            for (CollectionDefinition definition : CollectionManager.getCatalog().inCategory(category)) {
                CosmicItem item = CosmicItem.of(definition.getIcon(), category.getColor() + "&l" + definition.getDisplayName(),
                        "#AAAAAASpecific collection leaderboard.", "",
                        "#bdc8c9Click: #bbff6aView");
                entries.add(GuiIcon.of(item, click -> openCollection(click.getViewer(), definition)));
            }
            return entries;
        }
    }

    private static final class Ranking extends PaginatedGui {
        private final String accent;
        private final List<CollectionKeeper.Ranked> rows;
        private final CollectionCategory backTo;

        private Ranking(String title, String accent, List<CollectionKeeper.Ranked> rows, CollectionCategory backTo) {
            super(title, Menus.ROWS, Menus.contentSlots());
            this.accent = accent;
            this.rows = rows;
            this.backTo = backTo;
        }

        @Override
        protected void drawFrame(CosmicPlayer viewer) {
            Menus.shell(this, accent);

            int rank = -1;
            for (int i = 0; i < rows.size(); i++) {
                if (viewer.getUuid().equals(rows.get(i).uuid)) {
                    rank = i + 1;
                    break;
                }
            }
            setItem(4, CosmicItem.of("minecraft:player_head", "#00FC88Your Rank",
                    rank > 0 ? "#FFFFFF#" + rank : "#FFFFFFNot in the top " + CollectionManager.BOARD_SIZE)
                    .setSkullOwner(viewer.getUuid()));

            if (rows.isEmpty()) setItem(22, CosmicItem.of("minecraft:barrier", "#FF5555Nobody here yet"));

            setIcon(Menus.BACK_SLOT, Menus.back(() -> {
                if (backTo == null) openHub(viewer);
                else openCategory(viewer, backTo);
            }));
        }

        @Override
        protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
            List<GuiIcon> entries = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                CollectionKeeper.Ranked row = rows.get(i);
                CosmicItem head = CosmicItem.of("minecraft:player_head",
                        "#FFD700#" + (i + 1) + " #FFFFFF" + (row.name == null ? "?" : row.name),
                        "#AAAAAACollected: #00FC88" + Menus.grouped(row.amount)).setSkullOwner(row.uuid);
                entries.add(GuiIcon.of(head));
            }
            return entries;
        }
    }
}
