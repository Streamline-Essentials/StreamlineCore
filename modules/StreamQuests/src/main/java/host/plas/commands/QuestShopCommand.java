package host.plas.commands;

import host.plas.StreamQuests;
import host.plas.data.players.QuestPlayer;
import host.plas.data.shop.ShopCategory;
import host.plas.data.shop.ShopEntry;
import host.plas.data.shop.ShopManager;
import host.plas.gui.QuestShopMenu;
import host.plas.utils.Numbers;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiManager;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

/**
 * {@code /questshop (list|buy <item>)}: spend quest points. Without arguments it opens the shop
 * menu; {@code list} prints the offers and {@code buy} purchases one without a menu.
 */
public class QuestShopCommand extends ModuleCommand {
    public QuestShopCommand() {
        super(StreamQuests.getInstance(), "questshop", "streamline.command.questshop.default", "qshop", "questsshop");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        CosmicSender sender = context.getSender();
        if (sender == null || sender.isConsole()) {
            context.sendMessage("&cOnly players can use the quest shop.");
            return;
        }
        CosmicPlayer viewer = UserUtils.getOrCreatePlayer(sender).orElse(null);
        if (viewer == null) {
            context.sendMessage("&cFailed to load your quest points.");
            return;
        }

        String action = context.getArgCount() > 0 ? context.getStringArg(0).toLowerCase(Locale.ROOT) : "";
        switch (action) {
            case "":
            case "open":
                if (! GuiManager.isAvailable()) {
                    context.sendMessage("&cMenus are not available here. Use &f/questshop list &cand &f/questshop buy <item>&c.");
                    return;
                }
                withProgress(context, viewer, progress -> new QuestShopMenu(progress).open(viewer));
                return;
            case "list":
                withProgress(context, viewer, progress -> list(context, viewer, progress));
                return;
            case "buy":
                if (context.getArgCount() < 2) {
                    context.sendMessage("&cUsage: /questshop buy <item>");
                    return;
                }
                ShopEntry entry = ShopManager.getEntry(context.getStringArg(1)).orElse(null);
                if (entry == null || ! ShopManager.canSee(viewer, entry)) {
                    context.sendMessage("&cNo quest shop item named &f" + context.getStringArg(1) + "&c.");
                    return;
                }
                withProgress(context, viewer, progress -> ShopManager.purchase(viewer, progress, entry));
                return;
            default:
                context.sendMessage("&cUsage: /questshop (list|buy <item>)");
        }
    }

    private static void list(CommandContext<CosmicCommand> context, CosmicPlayer viewer, QuestPlayer progress) {
        StringBuilder out = new StringBuilder("&6&lQuest Shop &7(&e" + Numbers.format(progress.getPoints()) + " &7points)");
        for (ShopCategory category : ShopManager.getCategories()) {
            java.util.List<ShopEntry> entries = ShopManager.visibleEntries(viewer, category);
            if (entries.isEmpty()) continue;
            out.append("\n").append(category.getName()).append("&8:");
            for (ShopEntry entry : entries) {
                out.append("\n &8- &f").append(entry.getIdentifier()).append(" &8(").append(entry.getName())
                        .append("&8) &e").append(Numbers.format(entry.getPrice()));
            }
        }
        for (String line : out.toString().split("\n")) context.sendMessage(line);
    }

    private static void withProgress(CommandContext<CosmicCommand> context, CosmicPlayer viewer, Consumer<QuestPlayer> then) {
        CompletableFuture<QuestPlayer> future = StreamQuests.getLoader().getOrCreateAsync(viewer.getUuid());
        future.thenAccept(then).exceptionally(error -> {
            StreamQuests.getInstance().logWarning("Could not open the quest shop for " + viewer.getCurrentName() + ": " + error);
            context.sendMessage("&cFailed to load your quest points.");
            return null;
        });
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> options = new ConcurrentSkipListSet<>();
        if (context.getArgCount() <= 1) {
            options.add("list");
            options.add("buy");
            return ModuleUtils.getCompletion(options, context.getArgCount() == 1 ? context.getStringArg(0) : "");
        }
        if (context.getArgCount() == 2 && context.getStringArg(0).equalsIgnoreCase("buy")) {
            CosmicPlayer viewer = context.getSender() == null ? null
                    : UserUtils.getOrCreatePlayer(context.getSender()).orElse(null);
            for (ShopEntry entry : ShopManager.getEntries()) {
                if (viewer == null || ShopManager.canSee(viewer, entry)) options.add(entry.getIdentifier());
            }
            return ModuleUtils.getCompletion(options, context.getStringArg(1));
        }
        return options;
    }
}
