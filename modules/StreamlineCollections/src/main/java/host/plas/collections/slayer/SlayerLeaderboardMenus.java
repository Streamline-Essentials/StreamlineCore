package host.plas.collections.slayer;

import host.plas.collections.database.TrackKeeper;
import host.plas.collections.gui.Menus;
import host.plas.collections.gui.RankingMenu;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * {@code /slayerlb}: a hub with the overall board and one entry per category, each category
 * listing its total board and a board per mob.
 */
public final class SlayerLeaderboardMenus {
    private SlayerLeaderboardMenus() {
    }

    private static final String ACCENT = "#FF5555";

    public static void openHub(CosmicPlayer viewer) {
        new Hub().open(viewer);
    }

    public static void openCategory(CosmicPlayer viewer, SlayerCategory category) {
        new CategoryBoards(category).open(viewer);
    }

    public static void openOverall(CosmicPlayer viewer) {
        show(viewer, ACCENT + "&lOverall Slayer", ACCENT, SlayerManager.topOverall(), SlayerLeaderboardMenus::openHub);
    }

    public static void openCategoryTotal(CosmicPlayer viewer, SlayerCategory category) {
        show(viewer, category.getAccent() + "&l" + category.getDisplayName() + " Total", category.getAccent(),
                SlayerManager.top(category), player -> openCategory(player, category));
    }

    public static void openMob(CosmicPlayer viewer, SlayerDefinition definition) {
        SlayerCategory category = definition.getCategory();
        show(viewer, category.getAccent() + "&l" + definition.getDisplayName() + " LB", category.getAccent(),
                SlayerManager.top(definition), player -> openCategory(player, category));
    }

    private static void show(CosmicPlayer viewer, String title, String accent, CompletableFuture<List<TrackKeeper.Ranked>> board,
                             Consumer<CosmicPlayer> back) {
        board.whenComplete((rows, error) -> new RankingMenu(title, accent, rows,
                row -> "#AAAAAAKills: #00FC88" + Menus.grouped(row.amount), back, null).open(viewer));
    }

    private static final class Hub extends CosmicGui {
        private Hub() {
            super(ACCENT + "&lSlayer Leaderboards", Menus.ROWS);
        }

        @Override
        public void draw(CosmicPlayer viewer) {
            clear();
            Menus.shell(this, ACCENT);
            setIcon(13, CosmicItem.of("minecraft:netherite_sword", ACCENT + "&lOverall",
                    "#AAAAAATotal mobs slain across every category.", "", "#bdc8c9Click: #bbff6aView leaderboard"),
                    click -> openOverall(click.getViewer()));

            SlayerCategory[] categories = SlayerCategory.values();
            int[] slots = Menus.centredRow(2, categories.length);
            for (int i = 0; i < slots.length; i++) {
                SlayerCategory category = categories[i];
                setIcon(slots[i], CosmicItem.of(category.getIcon(), category.getAccent() + "&l" + category.getDisplayName(),
                        "#AAAAAABrowse this category's leaderboards.", "", "#bdc8c9Click: #bbff6aOpen"),
                        click -> openCategory(click.getViewer(), category));
            }
            setIcon(Menus.BACK_SLOT, Menus.close());
        }
    }

    private static final class CategoryBoards extends PaginatedGui {
        private final SlayerCategory category;

        private CategoryBoards(SlayerCategory category) {
            super(category.getAccent() + "&l" + category.getDisplayName() + " Leaderboards", Menus.ROWS, Menus.contentSlots());
            this.category = category;
        }

        @Override
        protected void drawFrame(CosmicPlayer viewer) {
            Menus.shell(this, category.getAccent());
            setIcon(4, CosmicItem.of("minecraft:gold_ingot", category.getAccent() + "&lAll " + category.getDisplayName(),
                    "#AAAAAACombined kills for this category.", "", "#bdc8c9Click: #bbff6aView leaderboard"),
                    click -> openCategoryTotal(click.getViewer(), category));
            setIcon(Menus.BACK_SLOT, Menus.back(() -> openHub(viewer)));
        }

        @Override
        protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
            List<GuiIcon> entries = new ArrayList<>();
            for (SlayerDefinition definition : SlayerCatalog.in(category)) {
                CosmicItem item = CosmicItem.of(definition.getIcon(), category.getAccent() + "&l" + definition.getDisplayName(),
                        "#AAAAAAKills of this mob.", "", "#bdc8c9Click: #bbff6aView");
                entries.add(GuiIcon.of(item, click -> openMob(click.getViewer(), definition)));
            }
            return entries;
        }
    }
}
