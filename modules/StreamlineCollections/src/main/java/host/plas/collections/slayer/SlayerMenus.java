package host.plas.collections.slayer;

import host.plas.collections.data.TrackedPlayer;
import host.plas.collections.database.TrackKeeper;
import host.plas.collections.gui.Menus;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.GuiLayout;
import singularity.gui.PaginatedGui;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code /slayer} menus: categories, then a category's mobs, then one mob's levels. Completed
 * levels are claimed by clicking them in the last menu, which only the owner of the kills can do.
 */
public final class SlayerMenus {
    private SlayerMenus() {
    }

    private static final String ACCENT = "#FF5555";
    private static final int MAIN_ROWS = 5;
    private static final List<Integer> LEVEL_SLOTS = GuiLayout.paddedSlots(Menus.ROWS, 3, 1, 1, 1);

    public static void openMain(CosmicPlayer viewer, TrackedPlayer progress) {
        new Main(progress).open(viewer);
    }

    /** Loads the mob's leaderboard, then opens its levels. */
    public static void openDetail(CosmicPlayer viewer, TrackedPlayer progress, SlayerDefinition definition) {
        SlayerManager.top(definition).whenComplete((top, error) ->
                new Detail(progress, definition, top == null ? new ArrayList<>() : top).open(viewer));
    }

    private static boolean own(CosmicPlayer viewer, TrackedPlayer progress) {
        return viewer.getUuid().equals(progress.getIdentifier());
    }

    private static String plural(int count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }

    private static final class Main extends CosmicGui {
        private final TrackedPlayer progress;

        private Main(TrackedPlayer progress) {
            super(ACCENT + "&lSlayer", MAIN_ROWS);
            this.progress = progress;
        }

        @Override
        public void draw(CosmicPlayer viewer) {
            clear();
            Menus.shell(this, ACCENT);
            boolean claimable = SlayerManager.canClaim(viewer, progress);

            SlayerManager.Totals overall = SlayerManager.Totals.of(progress, SlayerCatalog.all());
            List<String> lore = new ArrayList<>();
            if (! own(viewer, progress)) {
                lore.add("#AAAAAAViewing: #FFFFFF" + progress.getName());
                lore.add("");
            }
            lore.add("#AAAAAATotal Kills: #FFFFFF" + Menus.grouped(overall.getKills()));
            lore.add("#AAAAAATier Progress: #00FC88" + Menus.percent(overall.getTiers(), overall.getLevels()) + "%");
            lore.add("#AAAAAATiers: #00FC88" + overall.getTiers() + "#AAAAAA/#FFFFFF" + overall.getLevels());
            lore.add("#AAAAAAMaxed: #00FC88" + overall.getMaxed() + "#AAAAAA/#FFFFFF" + overall.getCount());
            int waiting = claimable ? SlayerManager.unclaimed(progress) : 0;
            if (waiting > 0) lore.add("#00FC88" + plural(waiting, "reward") + " waiting to claim!");
            lore.add("");
            lore.add("#AAAAAAClick a category below to view mobs.");
            setItem(4, CosmicItem.of("minecraft:iron_sword", ACCENT + "&lSlayer Progress").setLore(lore));

            SlayerCategory[] categories = SlayerCategory.values();
            int[] slots = Menus.centredRow(2, categories.length);
            for (int i = 0; i < slots.length; i++) {
                SlayerCategory category = categories[i];
                SlayerManager.Totals totals = SlayerManager.Totals.of(progress, SlayerCatalog.in(category));
                List<String> categoryLore = new ArrayList<>();
                categoryLore.add("#AAAAAAKills: #FFFFFF" + Menus.grouped(totals.getKills()));
                categoryLore.add("#AAAAAAProgress: #FFFFFF" + totals.getTiers() + "/" + totals.getLevels()
                        + " #AAAAAA(" + Menus.percent(totals.getTiers(), totals.getLevels()) + "%)");
                categoryLore.add("#AAAAAAMaxed: #FFFFFF" + totals.getMaxed() + "/" + totals.getCount());
                if (claimable && redeemable(category)) {
                    categoryLore.add("");
                    categoryLore.add("#00FC88You have slayer rewards you can redeem!");
                }
                categoryLore.add("");
                categoryLore.add("#38C1FCClick to view " + category.getDisplayName() + " mobs!");
                CosmicItem item = CosmicItem.of(category.getIcon(), category.getAccent() + "&l" + category.getDisplayName())
                        .setLore(categoryLore);
                setIcon(slots[i], item, click -> new Category(progress, category).open(click.getViewer()));
            }
            setIcon(GuiLayout.bottomCenter(MAIN_ROWS), Menus.close());
        }

        private boolean redeemable(SlayerCategory category) {
            for (SlayerDefinition definition : SlayerCatalog.in(category)) {
                if (SlayerManager.unclaimed(progress, definition) > 0) return true;
            }
            return false;
        }
    }

    private static final class Category extends PaginatedGui {
        private final TrackedPlayer progress;
        private final SlayerCategory category;

        private Category(TrackedPlayer progress, SlayerCategory category) {
            super(category.getAccent() + "&l" + category.getDisplayName() + " Slayer", Menus.ROWS, Menus.contentSlots());
            this.progress = progress;
            this.category = category;
        }

        @Override
        protected void drawFrame(CosmicPlayer viewer) {
            Menus.shell(this, category.getAccent());
            setIcon(Menus.BACK_SLOT, Menus.back(() -> new Main(progress).open(viewer)));
        }

        @Override
        protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
            boolean claimable = SlayerManager.canClaim(viewer, progress);
            List<GuiIcon> entries = new ArrayList<>();
            for (SlayerDefinition definition : SlayerCatalog.in(category)) {
                long kills = progress.amount(definition.getId());
                int tier = definition.tier(kills);
                List<String> lore = new ArrayList<>();
                lore.add("#AAAAAAKills: #FFFFFF" + Menus.grouped(kills));
                lore.add("#AAAAAATier: #FFFFFF" + tier + "/" + definition.levels()
                        + " #AAAAAA(" + Menus.percent(tier, definition.levels()) + "%)");
                lore.add(definition.maxed(kills) ? "#00FC88MAXED OUT"
                        : "#AAAAAANext tier at: #FFFFFF" + Menus.grouped(definition.next(kills)));
                int waiting = claimable ? SlayerManager.unclaimed(progress, definition) : 0;
                if (waiting > 0) lore.add("#00FC88" + plural(waiting, "reward") + " ready to claim!");
                lore.add("");
                lore.add("#38C1FCClick to view details!");
                CosmicItem item = CosmicItem.of(definition.getIcon(),
                        "#FFFFFF" + definition.getDisplayName() + (tier > 0 ? " " + Menus.roman(tier) : "")).setLore(lore);
                entries.add(GuiIcon.of(item, click -> openDetail(click.getViewer(), progress, definition)));
            }
            return entries;
        }
    }

    private static final class Detail extends CosmicGui {
        private final TrackedPlayer progress;
        private final SlayerDefinition definition;
        private final List<TrackKeeper.Ranked> top;

        private Detail(TrackedPlayer progress, SlayerDefinition definition, List<TrackKeeper.Ranked> top) {
            super("#FFFFFF&l" + definition.getDisplayName() + " Slayer", Menus.ROWS);
            this.progress = progress;
            this.definition = definition;
            this.top = top;
        }

        @Override
        public void draw(CosmicPlayer viewer) {
            clear();
            Menus.shell(this, definition.getCategory().getAccent());
            boolean own = own(viewer, progress);
            boolean claimable = SlayerManager.canClaim(viewer, progress);
            boolean rewards = SlayerManager.rewardsEnabled();
            long kills = progress.amount(definition.getId());
            int tier = definition.tier(kills);

            List<String> lore = new ArrayList<>();
            if (! own) {
                lore.add("#AAAAAAPlayer: #FFFFFF" + progress.getName());
                lore.add("");
            }
            lore.add("#AAAAAAKills: #FFFFFF" + Menus.grouped(kills));
            lore.add("#AAAAAATier: #FFFFFF" + tier + "/" + definition.levels() + " #AAAAAA(" + Menus.percent(tier, definition.levels()) + "%)");
            int waiting = claimable ? SlayerManager.unclaimed(progress, definition) : 0;
            if (waiting > 0) lore.add("#00FC88" + plural(waiting, "reward") + " ready to claim");
            lore.add(definition.maxed(kills) ? "#00FC88MAXED OUT" : "#AAAAAANext tier: #FFFFFF" + Menus.grouped(definition.next(kills)));
            setItem(13, CosmicItem.of(definition.getIcon(), ACCENT + "&l" + definition.getDisplayName()).setLore(lore));

            List<String> board = new ArrayList<>();
            board.add("#AAAAAATop 10 for " + definition.getDisplayName());
            board.add("");
            if (top.isEmpty()) board.add("#AAAAAANo entries yet.");
            for (int i = 0; i < top.size() && i < 10; i++) {
                TrackKeeper.Ranked entry = top.get(i);
                board.add("#FFD700#" + (i + 1) + " #FFFFFF" + (entry.name == null ? "?" : entry.name)
                        + " #AAAAAA- #FFFFFF" + Menus.grouped(entry.amount));
            }
            setItem(15, CosmicItem.of("minecraft:golden_sword", "#FFD700&lTop Slayers").setLore(board));

            for (int level = 1; level <= Math.min(definition.levels(), LEVEL_SLOTS.size()); level++) {
                drawLevel(LEVEL_SLOTS.get(level - 1), level, kills, tier, own, claimable, rewards);
            }
            setIcon(Menus.BACK_SLOT, Menus.back(() -> new Category(progress, definition.getCategory()).open(viewer)));
        }

        private void drawLevel(int slot, int level, long kills, int tier, boolean own, boolean claimable, boolean rewards) {
            long threshold = definition.threshold(level);
            boolean completed = kills >= threshold;
            boolean claimed = completed && progress.isClaimed(definition.getId(), level);
            boolean ready = completed && ! claimed && claimable && rewards;
            String reward = SlayerManager.getConfig().getReward(level).getDescription();
            String label = "Level " + level + " #AAAAAA(" + Menus.roman(level) + ")";

            List<String> lore = new ArrayList<>();
            lore.add("#AAAAAARequirement: #FFFFFF" + Menus.grouped(threshold) + " kills");
            String material;
            if (ready) {
                material = "minecraft:lime_stained_glass_pane";
                lore.add("#00FC88Ready to claim");
            } else if (completed) {
                material = "minecraft:green_stained_glass_pane";
                lore.add(claimed ? "#AAAAAAClaimed" : "#00FC88Completed");
            } else if (level == tier + 1) {
                material = "minecraft:yellow_stained_glass_pane";
                lore.add("#FFED6ACurrent Goal");
            } else {
                material = "minecraft:red_stained_glass_pane";
                lore.add("#FF5555Locked");
            }
            if (rewards && ! reward.isEmpty()) lore.add("#AAAAAAReward: " + reward);
            if (ready) {
                lore.add("");
                lore.add("#bbff6aClick to claim reward");
            } else if (completed && claimed && own && rewards) {
                lore.add("");
                lore.add("#bdc8c9Already claimed");
            }

            CosmicItem item = CosmicItem.of(material, (ready ? "#00FC88&l" : "#FFFFFF") + label).setLore(lore);
            if (ready) {
                item.setGlowing(true);
                setIcon(slot, item, click -> claim(click.getViewer(), level));
            } else {
                setItem(slot, item);
            }
        }

        private void claim(CosmicPlayer viewer, int level) {
            switch (SlayerManager.claim(viewer, definition, level)) {
                case CLAIMED:
                    redraw();
                    break;
                case ALREADY_CLAIMED:
                    ModuleUtils.sendMessage(viewer, SlayerManager.message("already-claimed"));
                    redraw();
                    break;
                case DISABLED:
                    ModuleUtils.sendMessage(viewer, SlayerManager.message("rewards-disabled"));
                    break;
                case ELSEWHERE:
                    ModuleUtils.sendMessage(viewer, SlayerManager.message("claim-elsewhere"));
                    break;
                case LOCKED:
                default:
                    ModuleUtils.sendMessage(viewer, SlayerManager.message("locked"));
                    break;
            }
        }
    }
}
