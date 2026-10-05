package host.plas.collections.commands;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.CollectionPlayer;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /collectionsadmin <reload|give <player> <collection> <amount>|set <player> <collection> <amount>|wipe <player>>}.
 * Changes to progress apply to players loaded on this server, whose live progress would
 * otherwise overwrite a change made to the database.
 */
public class CollectionsAdminCommand extends ModuleCommand {
    private static final String USAGE = "&cUsage: /collectionsadmin <reload|give <player> <collection> <amount>"
            + "|set <player> <collection> <amount>|wipe <player>>";

    public CollectionsAdminCommand() {
        super(StreamlineCollections.getInstance(), "collectionsadmin", "streamline.command.collectionsadmin.default",
                "coladmin");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        if (args.length == 0) {
            context.sendMessage(USAGE);
            return;
        }

        String action = args[0].toLowerCase();
        if (action.equals("reload")) {
            StreamlineCollections.reload();
            context.sendMessage("&aReloaded &eStreamlineCollections &aconfig and catalog.");
            return;
        }

        if (! StreamlineCollections.isTrackingServer()) {
            context.sendMessage("&cProgress can only be changed on a game server.");
            return;
        }

        if (args.length < 2) {
            context.sendMessage(USAGE);
            return;
        }

        Optional<CollectionPlayer> progress = UserUtils.getUUIDFromName(args[1]).flatMap(CollectionManager::getLoaded);
        if (progress.isEmpty()) {
            context.sendMessage("&c" + args[1] + " is not online on this server.");
            return;
        }

        if (action.equals("wipe")) {
            progress.get().wipe();
            progress.get().save(true);
            CollectionManager.clearBoards();
            context.sendMessage("&aWiped the collections of &e" + args[1] + "&a.");
            return;
        }

        if ((! action.equals("give") && ! action.equals("set")) || args.length < 4) {
            context.sendMessage(USAGE);
            return;
        }

        Optional<CollectionDefinition> definition = CollectionManager.getCatalog().get(args[2]);
        if (definition.isEmpty()) {
            context.sendMessage(CollectionManager.message("unknown").replace("%collection%", args[2]));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            context.sendMessage("&c'" + args[3] + "' is not a number.");
            return;
        }

        String id = definition.get().getId();
        if (action.equals("give")) {
            Optional<singularity.data.players.CosmicPlayer> online = UserUtils.getOrGetPlayer(progress.get().getIdentifier());
            if (online.isPresent()) CollectionManager.add(online.get(), id, amount);
            else progress.get().add(id, amount);
        } else {
            progress.get().set(id, amount);
        }
        context.sendMessage("&e" + args[1] + "&a now has &e" + progress.get().amount(id) + " &ain &e" + definition.get().getDisplayName() + "&a.");
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        List<String> options = new ArrayList<>();

        if (args.length <= 1) {
            options.add("reload");
            options.add("give");
            options.add("set");
            options.add("wipe");
        } else if (args.length == 2 && ! args[0].equalsIgnoreCase("reload")) {
            options.addAll(ModuleUtils.getOnlinePlayerNames());
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set"))) {
            for (CollectionDefinition definition : CollectionManager.getCatalog().all()) options.add(definition.getId());
        }

        return Commands.complete(options, args);
    }
}
