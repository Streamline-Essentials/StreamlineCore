package host.plas.collections.progression;

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

/** {@code /progressionleaderboard (stars|total|<category>)}: the progression boards. */
public class ProgressionBoardCommand extends ModuleCommand {
    public ProgressionBoardCommand() {
        super(StreamlineCollections.getInstance(), "progressionleaderboard", "streamline.command.progressionleaderboard.default",
                "progressionlb", "plb");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        Optional<CosmicPlayer> optionalViewer = Commands.menuViewer(context.getSender());
        if (optionalViewer.isEmpty()) return;
        CosmicPlayer viewer = optionalViewer.get();
        String[] args = Commands.args(context);
        String choice = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        switch (choice) {
            case "":
                ProgressionMenus.openHub(viewer);
                break;
            case "stars":
                ProgressionMenus.openBoard(viewer, null, true);
                break;
            case "total":
            case "exp":
            case "total-exp":
                ProgressionMenus.openBoard(viewer, null, false);
                break;
            default: {
                ProgressionStat stat = ProgressionStat.byKey(choice);
                if (stat == null) {
                    context.sendMessage(ProgressionManager.message("unknown-category").replace("%category%", args[0]));
                    return;
                }
                ProgressionMenus.openBoard(viewer, stat, false);
                break;
            }
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.add("stars");
            options.add("total");
            options.addAll(ProgressionManager.categoryKeys());
        }
        return Commands.complete(options, args);
    }
}
