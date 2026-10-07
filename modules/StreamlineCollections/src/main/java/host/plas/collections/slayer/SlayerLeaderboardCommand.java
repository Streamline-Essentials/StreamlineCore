package host.plas.collections.slayer;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.commands.Commands;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /slayerlb (overall|<category> (total|<mob>)|<mob>)}
 */
public class SlayerLeaderboardCommand extends ModuleCommand {
    public SlayerLeaderboardCommand() {
        super(StreamlineCollections.getInstance(), "slayerlb", "streamline.command.slayerlb.default",
                "slayerleaderboard", "slayerleaderboards");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        Optional<CosmicPlayer> optionalViewer = Commands.menuViewer(context.getSender());
        if (optionalViewer.isEmpty()) return;
        CosmicPlayer viewer = optionalViewer.get();
        String[] args = Commands.args(context);

        if (args.length == 0) {
            SlayerLeaderboardMenus.openHub(viewer);
            return;
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("overall") || first.equals("all") || first.equals("total")) {
            SlayerLeaderboardMenus.openOverall(viewer);
            return;
        }
        SlayerCategory category = SlayerCategory.byName(first);
        if (category != null) {
            if (args.length >= 2) {
                String second = args[1].toLowerCase(Locale.ROOT);
                if (second.equals("total") || second.equals("all")) {
                    SlayerLeaderboardMenus.openCategoryTotal(viewer, category);
                    return;
                }
                SlayerDefinition definition = SlayerCatalog.get(second);
                if (definition != null && definition.getCategory() == category) {
                    SlayerLeaderboardMenus.openMob(viewer, definition);
                    return;
                }
            }
            SlayerLeaderboardMenus.openCategory(viewer, category);
            return;
        }
        SlayerDefinition definition = SlayerCatalog.get(first);
        if (definition != null) SlayerLeaderboardMenus.openMob(viewer, definition);
        else SlayerLeaderboardMenus.openHub(viewer);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.add("overall");
            for (SlayerCategory category : SlayerCategory.values()) options.add(category.id());
            options.addAll(SlayerCatalog.ids());
        } else if (args.length == 2) {
            SlayerCategory category = SlayerCategory.byName(args[0]);
            if (category != null) {
                options.add("total");
                for (SlayerDefinition definition : SlayerCatalog.in(category)) options.add(definition.getId());
            }
        }
        return Commands.complete(options, args);
    }
}
