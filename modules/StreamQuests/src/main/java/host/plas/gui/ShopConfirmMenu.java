package host.plas.gui;

import host.plas.data.players.QuestPlayer;
import host.plas.data.shop.ShopCategory;
import host.plas.data.shop.ShopEntry;
import host.plas.data.shop.ShopManager;
import host.plas.utils.Numbers;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiLayout;

/** Asks before spending points: the entry in the middle, confirm on the left, cancel on the right. */
public class ShopConfirmMenu extends CosmicGui {
    private static final int ROWS = 3;

    private final QuestPlayer progress;
    private final ShopCategory category;
    private final ShopEntry entry;

    public ShopConfirmMenu(QuestPlayer progress, ShopCategory category, ShopEntry entry) {
        super("&8Buy " + entry.getName() + "&8?", ROWS);
        this.progress = progress;
        this.category = category;
        this.entry = entry;
    }

    @Override
    public void draw(CosmicPlayer viewer) {
        clear();
        GuiLayout.fillShell(this, null);

        setItem(GuiLayout.slot(1, 4), ShopCategoryMenu.icon(entry, progress, false));

        boolean affordable = progress.getPoints() >= entry.getPrice();
        CosmicItem confirm = affordable
                ? CosmicItem.of("minecraft:lime_concrete", "&a&lConfirm",
                        "&7Spend &e" + Numbers.format(entry.getPrice()) + " &7points.",
                        "&7You will have &e" + Numbers.format(progress.getPoints() - entry.getPrice()) + " &7left.")
                : CosmicItem.of("minecraft:gray_concrete", "&7&lConfirm",
                        "&cYou need &e" + Numbers.format(entry.getPrice() - progress.getPoints()) + " &cmore points.");
        setIcon(GuiLayout.slot(1, 2), confirm, click -> {
            ShopManager.purchase(click.getViewer(), progress, entry);
            click.open(new ShopCategoryMenu(progress, category));
        });

        setIcon(GuiLayout.slot(1, 6), CosmicItem.of("minecraft:red_concrete", "&c&lCancel", "&7Back to the shop."),
                click -> click.open(new ShopCategoryMenu(progress, category)));
    }
}
