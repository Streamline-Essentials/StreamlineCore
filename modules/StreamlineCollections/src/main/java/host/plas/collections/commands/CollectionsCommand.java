package host.plas.collections.commands;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.gui.DetailMenu;
import host.plas.collections.gui.MainMenu;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /collections ((player)|open <collection>)}: opens the collections menu, another
 * player's, or straight to one collection.
 */
public class CollectionsCommand extends ModuleCommand {
    public static final String OTHERS_PERMISSION = "streamline.command.collections.others";

    public CollectionsCommand() {
        super(StreamlineCollections.getInstance(), "collections", "streamline.command.collections.default",
                "collection", "col");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        Optional<CosmicPlayer> optionalViewer = Commands.menuViewer(context.getSender());
        if (optionalViewer.isEmpty()) return;
        CosmicPlayer viewer = optionalViewer.get();
        String[] args = Commands.args(context);

        if (args.length >= 1 && args[0].equalsIgnoreCase("open")) {
            String id = args.length >= 2 ? args[1] : "";
            Optional<CollectionDefinition> definition = CollectionManager.getCatalog().get(id);
            if (definition.isEmpty()) {
                context.sendMessage(CollectionManager.message("unknown").replace("%collection%", id));
                return;
            }
            CollectionManager.snapshot(viewer.getUuid())
                    .thenAccept(progress -> DetailMenu.openFor(viewer, progress, definition.get()));
            return;
        }

        if (args.length >= 1 && ! args[0].equalsIgnoreCase(viewer.getCurrentName())) {
            if (! ModuleUtils.hasPermission(viewer, OTHERS_PERMISSION)) {
                context.sendMessage(CollectionManager.message("no-permission"));
                return;
            }
            String name = args[0];
            Optional<String> uuid = UserUtils.getUUIDFromName(name);
            if (uuid.isEmpty()) {
                context.sendMessage(CollectionManager.message("unknown-player").replace("%player%", name));
                return;
            }
            CollectionManager.snapshot(uuid.get()).thenAccept(progress -> {
                if (progress.getName() == null || progress.getName().isEmpty()) progress.setName(name);
                new MainMenu(progress).open(viewer);
            });
            return;
        }

        CollectionManager.snapshot(viewer.getUuid()).thenAccept(progress -> new MainMenu(progress).open(viewer));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        List<String> options = new ArrayList<>();

        if (args.length <= 1) {
            options.add("open");
            if (ModuleUtils.hasPermission(context.getSender(), OTHERS_PERMISSION)) {
                options.addAll(ModuleUtils.getOnlinePlayerNames());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            for (CollectionDefinition definition : CollectionManager.getCatalog().all()) options.add(definition.getId());
        }

        return Commands.complete(options, args);
    }
}
