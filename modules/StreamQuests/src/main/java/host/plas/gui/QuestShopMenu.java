package host.plas.gui;

import host.plas.configs.ShopConfig;
import host.plas.data.players.QuestPlayer;
import host.plas.data.shop.ShopCategory;
import host.plas.data.shop.ShopManager;
import host.plas.utils.Numbers;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.GuiItems;
import singularity.gui.GuiLayout;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;

/** The quest shop's front page: the viewer's points and one icon per category. */
public class QuestShopMenu extends PaginatedGui {
    static final int ROWS = 6;

    private final QuestPlayer progress;

    public QuestShopMenu(QuestPlayer progress) {
        super(ShopConfig.getTitle(), ROWS);
        this.progress = progress;
    }

    @Override
    protected void drawFrame(CosmicPlayer viewer) {
        setItem(GuiLayout.defaultHeaderSlot(), pointsItem(progress));
        setIcon(GuiLayout.defaultBackSlot(ROWS), GuiIcon.close(GuiItems.closeButton()));
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        List<GuiIcon> entries = new ArrayList<>();
        for (ShopCategory category : ShopManager.getCategories()) {
            int visible = ShopManager.visibleEntries(viewer, category).size();
            if (visible == 0) continue;

            List<String> lore = new ArrayList<>(category.getDescription());
            if (! lore.isEmpty()) lore.add("");
            lore.add("&7Offers&8: &f" + visible);
            lore.add("");
            lore.add("&eClick to browse");

            CosmicItem item = CosmicItem.of(category.getIcon(), category.getName()).setLore(lore);
            item.setHideExtras(true);
            entries.add(GuiIcon.of(item, click -> click.open(new ShopCategoryMenu(progress, category))));
        }
        return entries;
    }

    static CosmicItem pointsItem(QuestPlayer progress) {
        return CosmicItem.of("minecraft:nether_star", "&e&lYour Quest Points",
                "&7Balance&8: &e" + Numbers.format(progress.getPoints()),
                "",
                "&7Earn points by completing &f/quests&7.");
    }
}
