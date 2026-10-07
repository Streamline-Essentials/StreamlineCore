package host.plas.collections.progression;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.commands.Commands;
import host.plas.collections.data.StatReader;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code /progressionadmin sync <all|<player>>}: raises players online here to their vanilla
 * statistics now, whether or not {@code sync-stats} is on.
 */
public class ProgressionAdminCommand extends ModuleCommand {
    public ProgressionAdminCommand() {
        super(StreamlineCollections.getInstance(), "progressionadmin", "streamline.command.progressionadmin.default", "progadmin");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String[] args = Commands.args(context);
        if (args.length < 2 || ! args[0].equalsIgnoreCase("sync")) {
            context.sendMessage(ProgressionManager.message("admin-usage"));
            return;
        }

        List<CosmicPlayer> players = new ArrayList<>();
        if (args[1].equalsIgnoreCase("all")) {
            players.addAll(UserUtils.getOnlinePlayers().values());
        } else {
            Optional<CosmicPlayer> player = Commands.uuidOf(args[1]).flatMap(UserUtils::getOrGetPlayer);
            if (player.isEmpty() || ! player.get().isOnline() || ProgressionManager.getLoaded(player.get().getUuid()).isEmpty()) {
                context.sendMessage(ProgressionManager.message("not-online").replace("%player%", args[1]));
                return;
            }
            players.add(player.get());
        }

        AtomicInteger synced = new AtomicInteger();
        StatReader.submit("sync progression", () -> {
            for (CosmicPlayer player : players) {
                if (ProgressionManager.sync(player)) synced.incrementAndGet();
            }
        }).thenRun(() -> context.sendMessage(ProgressionManager.message("sync-done").replace("%count%", String.valueOf(synced.get()))));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.add("sync");
        } else if (args.length == 2) {
            options.add("all");
            options.addAll(ModuleUtils.getOnlinePlayerNames());
        }
        return Commands.complete(options, args);
    }
}
