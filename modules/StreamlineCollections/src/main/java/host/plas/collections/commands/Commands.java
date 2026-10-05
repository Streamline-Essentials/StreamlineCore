package host.plas.collections.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiManager;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Helpers shared by the collections commands.
 */
final class Commands {
    private Commands() {
    }

    /** The sender as a player who can be shown a menu, telling them why not otherwise. */
    static Optional<CosmicPlayer> menuViewer(CosmicSender sender) {
        if (sender == null || sender.isConsole()) {
            ModuleUtils.sendMessage(sender, host.plas.collections.data.CollectionManager.message("players-only"));
            return Optional.empty();
        }
        if (! GuiManager.isAvailable()) {
            ModuleUtils.sendMessage(sender, host.plas.collections.data.CollectionManager.message("no-gui"));
            return Optional.empty();
        }
        return UserUtils.getOrCreatePlayer(sender);
    }

    /**
     * The arguments typed, without blank ones: a trailing or doubled space arrives as an empty
     * argument on some platforms, and would otherwise be read as a player or collection name.
     */
    static String[] args(CommandContext<CosmicCommand> context) {
        List<String> args = new ArrayList<>();
        for (String arg : context.getArgsArray()) {
            if (arg != null && ! arg.trim().isEmpty()) args.add(arg.trim());
        }
        return args.toArray(new String[0]);
    }

    /** The options that start with what is typed so far. */
    static ConcurrentSkipListSet<String> complete(Collection<String> options, String[] args) {
        String partial = args.length == 0 ? "" : args[args.length - 1];
        return ModuleUtils.getCompletion(new ConcurrentSkipListSet<>(options), partial);
    }
}
