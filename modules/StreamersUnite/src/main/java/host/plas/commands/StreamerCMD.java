package host.plas.commands;

import host.plas.StreamersUnite;
import host.plas.data.LiveManager;
import host.plas.data.StreamerSetup;
import host.plas.managers.StreamerUtils;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /streamer <add|remove|list> (player)}: manages who counts as a streamer. Adding
 * creates an empty setup in {@code streamers.yml}; {@code /sustreamer} edits the rest of it.
 */
public class StreamerCMD extends ModuleCommand {
    public StreamerCMD() {
        super(StreamersUnite.getInstance(),
                "streamer",
                "streamersunite.command.streamer",
                "streamers");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String action = context.isArgUsable(0) ? context.getStringArg(0).toLowerCase(Locale.ROOT) : "";
        switch (action) {
            case "add":
            case "remove": {
                if (! context.isArgUsable(1)) {
                    context.sendMessage(message("usage-streamer"));
                    return;
                }
                String name = context.getStringArg(1);
                Optional<CosmicSender> target = StreamerUtils.getOrGetSenderByName(name);
                if (target.isEmpty()) {
                    context.sendMessage(message("unknown-player").replace("%player%", name));
                    return;
                }
                CosmicSender player = target.get();
                String shownName = player.getCurrentName() == null || player.getCurrentName().isEmpty() ? name : player.getCurrentName();
                boolean registered = StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).isPresent();

                if (action.equals("add")) {
                    if (registered) {
                        context.sendMessage(message("already-added").replace("%player%", shownName));
                        return;
                    }
                    new StreamerSetup(UUID.fromString(player.getUuid()), new ArrayList<>(), new ArrayList<>(), "").save();
                    context.sendMessage(message("added").replace("%player%", shownName));
                } else {
                    if (! registered) {
                        context.sendMessage(message("not-registered").replace("%player%", shownName));
                        return;
                    }
                    StreamersUnite.getStreamerConfig().deleteSetup(player.getUuid());
                    LiveManager.goOffline(player.getUuid());
                    context.sendMessage(message("removed").replace("%player%", shownName));
                }
                return;
            }
            case "list": {
                List<String> names = streamerNames();
                context.sendMessage(message("list").replace("%streamers%", names.isEmpty() ? "-" : String.join(", ", names)));
                return;
            }
            default:
                context.sendMessage(message("usage-streamer"));
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> options = new ConcurrentSkipListSet<>();
        if (context.getArgCount() <= 1) {
            options.add("add");
            options.add("remove");
            options.add("list");
        } else if (context.getArgCount() == 2) {
            if (context.getStringArg(0).equalsIgnoreCase("remove")) options.addAll(streamerNames());
            else if (context.getStringArg(0).equalsIgnoreCase("add")) options.addAll(ModuleUtils.getOnlinePlayerNames());
        }
        return options;
    }

    /** The names of every registered streamer, sorted; streamers with no known name are left out. */
    static List<String> streamerNames() {
        List<String> names = new ArrayList<>();
        for (StreamerSetup setup : StreamersUnite.getStreamerConfig().getSetups()) {
            StreamerUtils.getOrGetSender(setup.getStreamerUuid().toString()).ifPresent(sender -> {
                String name = sender.getCurrentName();
                if (name != null && ! name.isBlank()) names.add(name);
            });
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static String message(String key) {
        return StreamersUnite.getMainConfig().getMessage(key);
    }
}
