package host.plas.collections.commands;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.Catalog;
import host.plas.collections.data.CollectionCategory;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.gui.LeaderboardMenus;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /collectionsleaderboard (overall|<category> (total|<collection>)|<collection>)}: opens
 * the leaderboard hub, the overall board, a category's boards or total, or one collection's board.
 */
public class CollectionsLeaderboardCommand extends ModuleCommand {
    public CollectionsLeaderboardCommand() {
        super(StreamlineCollections.getInstance(), "collectionsleaderboard", "streamline.command.collectionsleaderboard.default",
                "collectionleaderboard", "colleaderboard", "clb", "collectionslb");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        Optional<CosmicPlayer> optionalViewer = Commands.menuViewer(context.getSender());
        if (optionalViewer.isEmpty()) return;
        CosmicPlayer viewer = optionalViewer.get();
        String[] args = Commands.args(context);
        Catalog catalog = CollectionManager.getCatalog();

        if (args.length == 0) {
            LeaderboardMenus.openHub(viewer);
            return;
        }

        String first = args[0];
        if (first.equalsIgnoreCase("overall") || first.equalsIgnoreCase("all") || first.equalsIgnoreCase("total")) {
            LeaderboardMenus.openOverall(viewer);
            return;
        }

        Optional<CollectionCategory> category = catalog.getCategory(first);
        if (category.isPresent()) {
            if (args.length >= 2) {
                String second = args[1];
                if ((second.equalsIgnoreCase("total") || second.equalsIgnoreCase("all")) && category.get().isSummable()) {
                    LeaderboardMenus.openCategoryTotal(viewer, category.get());
                    return;
                }
                Optional<CollectionDefinition> definition = catalog.get(second);
                if (definition.isPresent() && definition.get().getCategory() == category.get()) {
                    LeaderboardMenus.openCollection(viewer, definition.get());
                    return;
                }
            }
            LeaderboardMenus.openCategory(viewer, category.get());
            return;
        }

        Optional<CollectionDefinition> definition = catalog.get(first);
        if (definition.isPresent()) {
            LeaderboardMenus.openCollection(viewer, definition.get());
            return;
        }

        LeaderboardMenus.openHub(viewer);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        Catalog catalog = CollectionManager.getCatalog();
        List<String> options = new ArrayList<>();

        if (args.length <= 1) {
            options.add("overall");
            for (CollectionCategory category : catalog.getCategories().values()) options.add(category.getId());
            for (CollectionDefinition definition : catalog.all()) options.add(definition.getId());
        } else if (args.length == 2) {
            catalog.getCategory(args[0]).ifPresent(category -> {
                if (category.isSummable()) options.add("total");
                for (CollectionDefinition definition : catalog.inCategory(category)) options.add(definition.getId());
            });
        }

        return Commands.complete(options, args);
    }
}
