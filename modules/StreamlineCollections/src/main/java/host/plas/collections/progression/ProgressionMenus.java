package host.plas.collections.progression;

import host.plas.collections.data.TrackedPlayer;
import host.plas.collections.gui.Menus;
import host.plas.collections.gui.RankingMenu;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;

import java.util.List;
import java.util.Locale;

/** The {@code /progression} menu and the {@code /progressionleaderboard} menus. */
public final class ProgressionMenus {
    private ProgressionMenus() {
    }

    private static final int STARS_SLOT = 4;
    private static final int LEADERBOARDS_SLOT = 49;
    private static final int HUB_STARS_SLOT = 3;
    private static final int HUB_OWN_SLOT = 4;
    private static final int HUB_TOTAL_SLOT = 5;

    public static void openProgress(CosmicPlayer viewer, TrackedPlayer progress) {
        new Progress(progress).open(viewer);
    }

    /** Loads the viewer's own progression, then shows it. */
    public static void openOwn(CosmicPlayer viewer) {
        ProgressionManager.snapshot(viewer.getUuid()).thenAccept(progress -> {
            if (progress.getName() == null || progress.getName().isEmpty()) progress.setName(viewer.getCurrentName());
            openProgress(viewer, progress);
        });
    }

    public static void openHub(CosmicPlayer viewer) {
        new Hub().open(viewer);
    }

    /**
     * Opens one board: a category, or total XP when {@code stat} is null, shown as stars when
     * {@code stars} is set.
     */
    public static void openBoard(CosmicPlayer viewer, ProgressionStat stat, boolean stars) {
        String title = title(stat, stars);
        String label = plain(title);
        ProgressionManager.board(stat).whenComplete((rows, error) -> new RankingMenu(title, accent(stat, stars), rows, row -> {
            String value = stars
                    ? "#FFD700★ " + StarsFormula.starsFromTotalXp(row.score)
                    : ProgressionManager.format(row.score) + (stat == null ? " XP" : "");
            return "#AAAAAA" + label + ": #00FC88" + value;
        }, ProgressionMenus::openHub, ProgressionMenus::openOwn).open(viewer));
    }

    private static String title(ProgressionStat stat, boolean stars) {
        if (stat != null) return "#00FC88" + stat.getDisplayName();
        return stars ? "#FFD700ѕᴛᴀʀѕ" : "#38C1FCᴛᴏᴛᴀʟ ᴇxᴘᴇʀɪᴇɴᴄᴇ";
    }

    private static String accent(ProgressionStat stat, boolean stars) {
        if (stat != null) return "#00FC88";
        return stars ? "#FFD700" : "#38C1FC";
    }

    private static String plain(String title) {
        return title.replaceAll("#[0-9A-Fa-f]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }

    static String bar(double progress) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, progress)) * 10);
        StringBuilder out = new StringBuilder("#38C1FC");
        for (int i = 0; i < filled; i++) out.append('█');
        out.append("#555555");
        for (int i = filled; i < 10; i++) out.append('█');
        return out.toString();
    }

    private static final class Progress extends CosmicGui {
        private final TrackedPlayer progress;

        private Progress(TrackedPlayer progress) {
            super("#FFD700ᴘʀᴏɢʀᴇѕѕɪᴏɴ", Menus.ROWS);
            this.progress = progress;
        }

        @Override
        public void draw(CosmicPlayer viewer) {
            clear();
            Menus.shell(this, "#FFD700");
            double total = ProgressionManager.totalXp(progress);
            long stars = StarsFormula.starsFromTotalXp(total);
            double toNext = StarsFormula.progressToNextStar(total);
            setItem(STARS_SLOT, CosmicItem.of("minecraft:nether_star", "#FFD700" + progress.getName() + "'s Stars",
                    "#AAAAAACurrent stars: #00FC88" + stars,
                    "#AAAAAATotal weighted XP: #FFFFFF" + ProgressionManager.format(total),
                    "",
                    "#AAAAAAProgress to next star",
                    bar(toNext),
                    "#AAAAAA" + ProgressionManager.format(StarsFormula.xpIntoCurrentStar(total)) + " / "
                            + ProgressionManager.format(StarsFormula.costForNextStar(stars))
                            + " XP (#FFFFFF" + Math.round(toNext * 100) + "%#AAAAAA)",
                    "",
                    "#38C1FCCosmetic progression only",
                    "#AAAAAANo combat or loot bonuses"));

            List<Integer> slots = Menus.contentSlots();
            int index = 0;
            for (ProgressionStat stat : ProgressionStat.values()) {
                if (index >= slots.size()) break;
                double amount = progress.amount(stat.key());
                double weight = ProgressionManager.weight(stat);
                setItem(slots.get(index++), CosmicItem.of(stat.getIcon(), "#00FC88" + stat.getDisplayName(),
                        "#AAAAAAAmount: #FFFFFF" + ProgressionManager.format(amount),
                        "#AAAAAAWeight: #FFFFFF" + ProgressionManager.format(weight) + "x",
                        "#AAAAAAWeighted XP: #38C1FC" + ProgressionManager.format(amount * weight)));
            }
            setIcon(LEADERBOARDS_SLOT, CosmicItem.of("minecraft:gold_ingot", "#FFD700ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅѕ", "#FFFFFFClick to view"),
                    click -> openHub(click.getViewer()));
        }
    }

    private static final class Hub extends CosmicGui {
        private Hub() {
            super("#FFD700ᴘʀᴏɢʀᴇѕѕɪᴏɴ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅѕ", Menus.ROWS);
        }

        @Override
        public void draw(CosmicPlayer viewer) {
            clear();
            Menus.shell(this, "#FFD700");
            setIcon(HUB_STARS_SLOT, CosmicItem.of("minecraft:nether_star", "#FFD700ѕᴛᴀʀѕ",
                    "#AAAAAAOverall star ranks.", "", "#FFFFFFClick to view"),
                    click -> openBoard(click.getViewer(), null, true));
            setIcon(HUB_OWN_SLOT, CosmicItem.of("minecraft:player_head", "#00FC88ᴠɪᴇᴡ ᴏᴡɴ ѕᴛᴀᴛѕ",
                    "#AAAAAAYour stars, categories and XP.", "", "#FFFFFFClick to view").setSkullOwner(viewer.getUuid()),
                    click -> openOwn(click.getViewer()));
            setIcon(HUB_TOTAL_SLOT, CosmicItem.of("minecraft:experience_bottle", "#38C1FCᴛᴏᴛᴀʟ ᴇxᴘᴇʀɪᴇɴᴄᴇ",
                    "#AAAAAAWeighted total of every category.", "", "#FFFFFFClick to view"),
                    click -> openBoard(click.getViewer(), null, false));

            List<Integer> slots = Menus.contentSlots();
            int index = 0;
            for (ProgressionStat stat : ProgressionStat.values()) {
                if (index >= slots.size()) break;
                setIcon(slots.get(index++), CosmicItem.of(stat.getIcon(), "#00FC88" + stat.getDisplayName(),
                        "#AAAAAATop players by " + stat.getDisplayName().toLowerCase(Locale.ROOT) + ".",
                        "#AAAAAAWeight: #FFFFFF" + ProgressionManager.format(ProgressionManager.weight(stat)) + "x",
                        "", "#FFFFFFClick to view"),
                        click -> openBoard(click.getViewer(), stat, false));
            }
            setIcon(Menus.BACK_SLOT, Menus.close());
        }
    }
}
