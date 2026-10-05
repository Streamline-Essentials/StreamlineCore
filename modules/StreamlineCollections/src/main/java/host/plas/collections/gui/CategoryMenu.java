package host.plas.collections.gui;

import host.plas.collections.data.CollectionCategory;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.CollectionPlayer;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;

/**
 * One category's collections, paged, each opening its {@link DetailMenu}.
 */
public class CategoryMenu extends PaginatedGui {
    private final CollectionPlayer progress;
    private final CollectionCategory category;

    public CategoryMenu(CollectionPlayer progress, CollectionCategory category) {
        super(category.getColor() + "&l" + category.getName() + " Collections", Menus.ROWS, Menus.contentSlots());
        this.progress = progress;
        this.category = category;
    }

    @Override
    protected void drawFrame(CosmicPlayer viewer) {
        Menus.shell(this, category.getColor());
        setIcon(Menus.BACK_SLOT, Menus.back(() -> new MainMenu(progress).open(viewer)));
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        boolean claimable = CollectionManager.canClaim(viewer, progress);
        List<GuiIcon> entries = new ArrayList<>();

        for (CollectionDefinition definition : CollectionManager.getCatalog().inCategory(category)) {
            long amount = progress.amount(definition.getId());
            int tier = definition.tier(amount);

            List<String> lore = new ArrayList<>();
            lore.add("#AAAAAACollected: #FFFFFF" + Menus.grouped(amount));
            lore.add("#AAAAAATier: #FFFFFF" + tier + "/" + definition.levels()
                    + " #AAAAAA(" + Menus.percent(tier, definition.levels()) + "%)");
            lore.add(definition.maxed(amount) ? "#00FC88MAXED OUT" : "#AAAAAANext tier at: #FFFFFF" + Menus.grouped(definition.next(amount)));
            if (claimable) {
                int unclaimed = CollectionManager.unclaimed(progress, definition);
                if (unclaimed > 0) lore.add("#00FC88" + unclaimed + (unclaimed == 1 ? " reward" : " rewards") + " ready to claim!");
            }
            lore.add("");
            lore.add("#38C1FCClick to view details!");

            String name = "#FFFFFF" + definition.getDisplayName() + (tier > 0 ? " " + Menus.roman(tier) : "");
            CosmicItem item = CosmicItem.of(definition.getIcon(), name).setLore(lore);
            entries.add(GuiIcon.of(item, click -> DetailMenu.openFor(click.getViewer(), progress, definition)));
        }
        return entries;
    }
}
