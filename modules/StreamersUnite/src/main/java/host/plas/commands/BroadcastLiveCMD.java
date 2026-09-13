package host.plas.commands;

import host.plas.StreamersUnite;
import host.plas.data.LiveManager;
import host.plas.managers.NotificationTimer;
import host.plas.managers.StreamerUtils;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class BroadcastLiveCMD extends ModuleCommand {
    public BroadcastLiveCMD() {
        super(StreamersUnite.getInstance(),
                "broadcast-live",
                "streamersunite.command.broadcast-live",
                "bclive");
    }

    @Override
    public void run(CommandContext<CosmicCommand> commandContext) {
        if (! commandContext.isPlayer()) {
            commandContext.getSender().sendMessage("&cYou must be a player to use this command!");
            return;
        }

        CosmicSender sender = commandContext.getSender();

        if (! commandContext.isArgUsable(0)) {
            if (StreamersUnite.getStreamerConfig().getSetup(sender.getUuid()).isPresent()) {
                if (LiveManager.isLive(sender)) {
                    commandContext.getSender().sendMessage("&eAttempting to broadcast your live link&8...");

                    if (! LiveManager.broadcastLive(sender)) {
                        commandContext.getSender().sendMessage("&cYou have already broadcasted your live link recently!");
                        return;
                    }

                    return;
                } else {
                    commandContext.getSender().sendMessage("&cYou must be live to broadcast your live link!");
                    return;
                }
            } else {
                commandContext.getSender().sendMessage("&cYou must be a streamer to use this command!");
                return;
            }
        }

        other(commandContext);
    }

    public static void other(CommandContext<CosmicCommand> commandContext) {
        CosmicSender sender = commandContext.getSender();
        if (! (sender instanceof CosmicPlayer)) {
            commandContext.getSender().sendMessage("&cYou must be a player to use this command!");
            return;
        }

        if (! sender.hasPermission("streamersunite.command.broadcast-live.others")) {
            commandContext.getSender().sendMessage("&cYou do not have permission to set other players' live status!");
            return;
        }

        if (! commandContext.isArgUsable(0)) {
            commandContext.getSender().sendMessage("&cYou must specify a player!");
            return;
        }

        String player = commandContext.getStringArg(0);

        Optional<CosmicSender> p = StreamerUtils.getOrGetSenderByName(player);
        if (p.isEmpty()) {
            commandContext.getSender().sendMessage("&cThat player does not exist!");
            return;
        }

        CosmicSender streamPlayer = p.get();

        if (StreamersUnite.getStreamerConfig().getSetup(streamPlayer.getUuid()).isEmpty()) {
            commandContext.getSender().sendMessage("&cThat player is not a streamer!");
            return;
        }

        if (! LiveManager.isLive(streamPlayer)) {
            commandContext.getSender().sendMessage("&cThat player is not live right now!");
            return;
        }

        commandContext.getSender().sendMessage("&eAttempting to broadcast &r" + streamPlayer.getCurrentName() + "&7'&es live link&8...");

        if (! LiveManager.broadcastLive(streamPlayer)) {
            commandContext.getSender().sendMessage("&cThat player has already broadcasted their live link recently!");
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext commandContext) {
        CosmicSender sender = commandContext.getSender();
        if (! sender.hasPermission("streamersunite.command.broadcast-live.others")) {
            if (NotificationTimer.hasNotification("broadcast-live", sender)) {
                return new ConcurrentSkipListSet<>();
            }

            commandContext.getSender().sendMessage("&cYou do not have permission to broadcast other players' live links!");

            NotificationTimer.addNotification("broadcast-live", sender);
            return new ConcurrentSkipListSet<>();
        }

        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        StreamersUnite.getStreamerConfig().getSetups().forEach(setup -> {
            Optional<CosmicSender> player = StreamerUtils.getOrGetSender(setup.getStreamerUuid().toString());

            player.ifPresent(streamSender -> {
                if (streamSender.getCurrentName() != null &&
                        ! streamSender.getCurrentName().isBlank() &&
                        ! streamSender.getCurrentName().isEmpty())
                    r.add(streamSender.getCurrentName());
            });
        });

        return r;
    }
}
