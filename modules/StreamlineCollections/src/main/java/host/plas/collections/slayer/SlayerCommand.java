package host.plas.collections.slayer;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.commands.Commands;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /slayer ((player)|open <mob>)}: opens the slayer menu, another player's, or straight to
 * one mob's levels.
 */
public class SlayerCommand extends ModuleCommand {
    public static final String OTHERS_PERMISSION = "streamline.command.slayer.others";

    public SlayerCommand() {
        super(StreamlineCollections.getInstance(), "slayer", "streamline.command.slayer.default");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        Optional<CosmicPlayer> optionalViewer = Commands.menuViewer(context.getSender());
        if (optionalViewer.isEmpty()) return;
        CosmicPlayer viewer = optionalViewer.get();
        String[] args = Commands.args(context);

        if (args.length >= 1 && args[0].equalsIgnoreCase("open")) {
            String id = args.length >= 2 ? args[1] : "";
            SlayerDefinition definition = SlayerCatalog.get(id);
            if (definition == null) {
                context.sendMessage(SlayerManager.message("unknown").replace("%mob%", id));
                return;
            }
            SlayerManager.snapshot(viewer.getUuid()).thenAccept(progress -> SlayerMenus.openDetail(viewer, progress, definition));
            return;
        }

        if (args.length >= 1 && ! args[0].equalsIgnoreCase(viewer.getCurrentName())) {
            if (! ModuleUtils.hasPermission(viewer, OTHERS_PERMISSION)) {
                context.sendMessage(SlayerManager.message("no-permission"));
                return;
            }
            String name = args[0];
            Optional<String> uuid = Commands.uuidOf(name);
            if (uuid.isEmpty()) {
                context.sendMessage(SlayerManager.message("unknown-player").replace("%player%", name));
                return;
            }
            SlayerManager.snapshot(uuid.get()).thenAccept(progress -> {
                if (progress.getName() == null || progress.getName().isEmpty()) progress.setName(name);
                SlayerMenus.openMain(viewer, progress);
            });
            return;
        }

        SlayerManager.snapshot(viewer.getUuid()).thenAccept(progress -> SlayerMenus.openMain(viewer, progress));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.add("open");
            if (ModuleUtils.hasPermission(context.getSender(), OTHERS_PERMISSION)) options.addAll(ModuleUtils.getOnlinePlayerNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            options.addAll(SlayerCatalog.ids());
        }
        return Commands.complete(options, args);
    }
}
