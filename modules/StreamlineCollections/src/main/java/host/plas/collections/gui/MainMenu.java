package host.plas.collections.gui;

import host.plas.collections.data.Catalog;
import host.plas.collections.data.CollectionCategory;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.TrackedPlayer;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /collections}: overall progress and one button per category.
 */
public class MainMenu extends CosmicGui {
    private static final String ACCENT = "#FFED6A";
    private static final int ROWS = 5;

    private final TrackedPlayer progress;

    public MainMenu(TrackedPlayer progress) {
        super(ACCENT + "&lCollections", ROWS);
        this.progress = progress;
    }

    @Override
    public void draw(CosmicPlayer viewer) {
        clear();
        Menus.shell(this, ACCENT);

        Catalog catalog = CollectionManager.getCatalog();
        boolean own = viewer.getUuid().equals(progress.getIdentifier());
        boolean claimable = own && CollectionManager.canClaim(viewer, progress);

        int tiers = 0;
        int levels = 0;
        int maxed = 0;
        for (CollectionDefinition definition : catalog.all()) {
            long amount = progress.amount(definition.getId());
            tiers += definition.tier(amount);
            levels += definition.levels();
            if (definition.maxed(amount)) maxed++;
        }

        List<String> lore = new ArrayList<>();
        if (! own) {
            lore.add("#AAAAAAViewing: #FFFFFF" + progress.getName());
            lore.add("");
        }
        lore.add("#AAAAAATier Progress: #00FC88" + Menus.percent(tiers, levels) + "%");
        lore.add("#AAAAAATiers: #00FC88" + tiers + "#AAAAAA/#FFFFFF" + levels);
        lore.add("#AAAAAAMaxed: #00FC88" + maxed + "#AAAAAA/#FFFFFF" + catalog.all().size());
        if (claimable) {
            int unclaimed = CollectionManager.unclaimed(progress);
            if (unclaimed > 0) lore.add("#00FC88" + unclaimed + (unclaimed == 1 ? " reward" : " rewards") + " waiting to claim!");
        }
        lore.add("");
        lore.add("#AAAAAAClick a category below to view collections.");
        setItem(4, CosmicItem.of("minecraft:book", ACCENT + "&lCollection Progress").setLore(lore));

        List<CollectionCategory> categories = new ArrayList<>(catalog.getCategories().values());
        int[] slots = Menus.centredRow(2, categories.size());
        for (int i = 0; i < slots.length; i++) {
            CollectionCategory category = categories.get(i);
            int categoryTiers = 0;
            int categoryLevels = 0;
            int categoryMaxed = 0;
            List<CollectionDefinition> inCategory = catalog.inCategory(category);
            for (CollectionDefinition definition : inCategory) {
                long amount = progress.amount(definition.getId());
                categoryTiers += definition.tier(amount);
                categoryLevels += definition.levels();
                if (definition.maxed(amount)) categoryMaxed++;
            }

            List<String> categoryLore = new ArrayList<>();
            categoryLore.add("#AAAAAAProgress: #FFFFFF" + categoryTiers + "/" + categoryLevels
                    + " #AAAAAA(" + Menus.percent(categoryTiers, categoryLevels) + "%)");
            categoryLore.add("#AAAAAAMaxed: #FFFFFF" + categoryMaxed + "/" + inCategory.size());
            if (claimable && redeemable(inCategory)) {
                categoryLore.add("");
                categoryLore.add("#00FC88You have collections you can redeem!");
            }
            categoryLore.add("");
            categoryLore.add("#38C1FCClick to view " + category.getName() + " collections!");
            CosmicItem item = CosmicItem.of(category.getIcon(), category.getColor() + "&l" + category.getName())
                    .setLore(categoryLore);
            setIcon(slots[i], item, click -> new CategoryMenu(progress, category).open(click.getViewer()));
        }

        setIcon(GuiLayout.bottomCenter(ROWS), Menus.close());
    }

    private boolean redeemable(List<CollectionDefinition> definitions) {
        for (CollectionDefinition definition : definitions) {
            if (CollectionManager.unclaimed(progress, definition) > 0) return true;
        }
        return false;
    }
}
