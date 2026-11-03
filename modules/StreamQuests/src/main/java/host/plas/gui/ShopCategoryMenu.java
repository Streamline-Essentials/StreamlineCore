package host.plas.gui;

import host.plas.configs.ShopConfig;
import host.plas.data.players.QuestPlayer;
import host.plas.data.shop.ShopCategory;
import host.plas.data.shop.ShopEntry;
import host.plas.data.shop.ShopManager;
import host.plas.data.shop.ShopReward;
import host.plas.utils.Numbers;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiClick;
import singularity.gui.GuiIcon;
import singularity.gui.GuiItems;
import singularity.gui.GuiLayout;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;

/** One quest shop category: its entries with price, rewards and whether the viewer can afford them. */
public class ShopCategoryMenu extends PaginatedGui {
    private final QuestPlayer progress;
    private final ShopCategory category;

    public ShopCategoryMenu(QuestPlayer progress, ShopCategory category) {
        super(category.getName(), QuestShopMenu.ROWS);
        this.progress = progress;
        this.category = category;
    }

    @Override
    protected void drawFrame(CosmicPlayer viewer) {
        setItem(GuiLayout.defaultHeaderSlot(), QuestShopMenu.pointsItem(progress));
        setIcon(GuiLayout.defaultBackSlot(QuestShopMenu.ROWS), GuiItems.returnButton(),
                click -> click.open(new QuestShopMenu(progress)));
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        List<GuiIcon> entries = new ArrayList<>();
        for (ShopEntry entry : ShopManager.visibleEntries(viewer, category)) {
            entries.add(GuiIcon.of(icon(entry, progress, true), click -> select(click, entry)));
        }
        return entries;
    }

    private void select(GuiClick click, ShopEntry entry) {
        if (ShopConfig.isConfirmPurchases()) {
            click.open(new ShopConfirmMenu(progress, category, entry));
            return;
        }
        ShopManager.purchase(click.getViewer(), progress, entry);
        click.redraw();
    }

    /** The entry as shown in the shop: description, rewards, price and the buy prompt. */
    static CosmicItem icon(ShopEntry entry, QuestPlayer progress, boolean prompt) {
        List<String> lore = new ArrayList<>(entry.getDescription());

        List<String> rewards = new ArrayList<>();
        for (ShopReward reward : entry.getRewards()) {
            String line = reward.describe();
            if (line != null) rewards.add(line);
        }
        if (! rewards.isEmpty()) {
            if (! lore.isEmpty()) lore.add("");
            lore.add("&7You get&8:");
            lore.addAll(rewards);
        }

        boolean affordable = progress.getPoints() >= entry.getPrice();
        if (! lore.isEmpty()) lore.add("");
        lore.add("&7Price&8: " + (affordable ? "&e" : "&c") + Numbers.format(entry.getPrice()) + " &7points");
        if (! entry.isAvailable()) {
            lore.add("&cNot available on this server.");
        } else if (prompt) {
            lore.add(affordable ? "&aClick to buy" : "&cYou need &e"
                    + Numbers.format(entry.getPrice() - progress.getPoints()) + " &cmore points");
        }

        CosmicItem item = CosmicItem.of(entry.getIcon(), entry.getName()).setLore(lore).setAmount(entry.getIconAmount());
        item.setGlowing(entry.isGlowing());
        item.setHideExtras(true);
        return item;
    }
}
