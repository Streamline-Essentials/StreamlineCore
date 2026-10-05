package host.plas.collections.gui;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.CollectionPlayer;
import host.plas.collections.data.LevelReward;
import host.plas.collections.database.CollectionKeeper;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiLayout;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * One collection: progress, its top collectors, and a pane per level that is clicked to claim.
 */
public class DetailMenu extends CosmicGui {
    private static final List<Integer> LEVEL_SLOTS = GuiLayout.paddedSlots(Menus.ROWS, 3, 1, 1, 1);

    private final CollectionPlayer progress;
    private final CollectionDefinition definition;
    private final List<CollectionKeeper.Ranked> board;

    public DetailMenu(CollectionPlayer progress, CollectionDefinition definition, List<CollectionKeeper.Ranked> board) {
        super("#FFFFFF&l" + definition.getDisplayName(), Menus.ROWS);
        this.progress = progress;
        this.definition = definition;
        this.board = board;
    }

    /** Loads the collection's leaderboard, then opens its menu. */
    public static void openFor(CosmicPlayer viewer, CollectionPlayer progress, CollectionDefinition definition) {
        CollectionManager.boardFor(definition).whenComplete((board, error) ->
                new DetailMenu(progress, definition, board == null ? new ArrayList<>() : board).open(viewer));
    }

    @Override
    public void draw(CosmicPlayer viewer) {
        clear();
        Menus.shell(this, definition.getCategory().getColor());

        boolean own = viewer.getUuid().equals(progress.getIdentifier());
        boolean claimable = CollectionManager.canClaim(viewer, progress);
        boolean rewards = StreamlineCollections.getMainConfig().isRewardsEnabled();
        long amount = progress.amount(definition.getId());
        int tier = definition.tier(amount);

        List<String> info = new ArrayList<>();
        if (! own) {
            info.add("#AAAAAAPlayer: #FFFFFF" + progress.getName());
            info.add("");
        }
        info.add("#AAAAAACollected: #FFFFFF" + Menus.grouped(amount));
        info.add("#AAAAAATier: #FFFFFF" + tier + "/" + definition.levels()
                + " #AAAAAA(" + Menus.percent(tier, definition.levels()) + "%)");
        if (claimable) {
            int unclaimed = CollectionManager.unclaimed(progress, definition);
            if (unclaimed > 0) info.add("#00FC88" + unclaimed + (unclaimed == 1 ? " reward" : " rewards") + " ready to claim");
        }
        info.add(definition.maxed(amount) ? "#00FC88MAXED OUT" : "#AAAAAANext tier: #FFFFFF" + Menus.grouped(definition.next(amount)));
        setItem(13, CosmicItem.of(definition.getIcon(), "#FFED6A&l" + definition.getDisplayName()).setLore(info));

        List<String> top = new ArrayList<>();
        top.add("#AAAAAATop 10 for " + definition.getDisplayName());
        top.add("");
        if (board.isEmpty()) {
            top.add("#AAAAAANo entries yet.");
        } else {
            for (int i = 0; i < Math.min(10, board.size()); i++) {
                CollectionKeeper.Ranked row = board.get(i);
                top.add("#FFD700#" + (i + 1) + " #FFFFFF" + (row.name == null ? "?" : row.name)
                        + " #AAAAAA- #FFFFFF" + Menus.grouped(row.amount));
            }
        }
        setItem(15, CosmicItem.of("minecraft:golden_helmet", "#FFD700&lTop Collectors").setLore(top));

        for (int level = 1; level <= Math.min(definition.levels(), LEVEL_SLOTS.size()); level++) {
            drawLevel(LEVEL_SLOTS.get(level - 1), level, tier, own, claimable, rewards);
        }

        setIcon(Menus.BACK_SLOT, Menus.back(() -> new CategoryMenu(progress, definition.getCategory()).open(viewer)));
    }

    private void drawLevel(int slot, int level, int tier, boolean own, boolean claimable, boolean rewards) {
        boolean completed = level <= tier;
        boolean claimed = progress.isClaimed(definition.getId(), level);
        LevelReward reward = StreamlineCollections.getMainConfig().getReward(level);
        String label = "Level " + level + " #AAAAAA(" + Menus.roman(level) + ")";

        List<String> lore = new ArrayList<>();
        lore.add("#AAAAAARequirement: #FFFFFF" + Menus.grouped(definition.threshold(level)));

        if (completed && ! claimed && claimable && rewards) {
            lore.add("#00FC88Ready to claim");
            if (! reward.getDescription().isEmpty()) lore.add("#AAAAAAReward: " + reward.getDescription());
            lore.add("");
            lore.add("#bbff6aClick to claim reward");
            CosmicItem item = CosmicItem.of("minecraft:lime_stained_glass_pane", "#00FC88&l" + label)
                    .setLore(lore).setGlowing(true);
            setIcon(slot, item, click -> claim(click.getViewer(), level));
            return;
        }

        String material;
        if (completed) {
            material = "minecraft:green_stained_glass_pane";
            lore.add(claimed ? "#AAAAAAClaimed" : "#00FC88Completed");
        } else if (level == tier + 1) {
            material = "minecraft:yellow_stained_glass_pane";
            lore.add("#FFED6ACurrent Goal");
        } else {
            material = "minecraft:red_stained_glass_pane";
            lore.add("#FF5555Locked");
        }
        if (rewards && ! reward.getDescription().isEmpty()) lore.add("#AAAAAAReward: " + reward.getDescription());
        if (completed && claimed && own && rewards) {
            lore.add("");
            lore.add("#bdc8c9Already claimed");
        }

        setItem(slot, CosmicItem.of(material, "#FFFFFF" + label).setLore(lore));
    }

    private void claim(CosmicPlayer viewer, int level) {
        switch (CollectionManager.claim(viewer, definition, level)) {
            case CLAIMED:
                redraw();
                break;
            case ALREADY_CLAIMED:
                ModuleUtils.sendMessage(viewer, CollectionManager.message("already-claimed"));
                redraw();
                break;
            case DISABLED:
                ModuleUtils.sendMessage(viewer, CollectionManager.message("rewards-disabled"));
                break;
            case ELSEWHERE:
                ModuleUtils.sendMessage(viewer, CollectionManager.message("claim-elsewhere"));
                break;
            case LOCKED:
            default:
                ModuleUtils.sendMessage(viewer, CollectionManager.message("locked"));
                break;
        }
    }
}
