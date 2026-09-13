package host.plas.commands;

import host.plas.data.LiveManager;
import host.plas.StreamersUnite;
import host.plas.managers.NotificationTimer;
import host.plas.managers.StreamerUtils;
import singularity.command.ModuleCommand;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class GoOfflineCMD extends ModuleCommand {
    public GoOfflineCMD() {
        super(StreamersUnite.getInstance(),
                "go-offline",
                "streamersunite.command.go-offline",
                "goffline", "offline", "gooffline");
    }

    @Override
    public void run(CommandContext<CosmicCommand> commandContext) {
        if (commandContext.isConsole()) {
            setOtherOffline(commandContext);
            return;
        }

        CosmicSender player = commandContext.getSender();

        if (! commandContext.isArgUsable(0)) {
            if (StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).isPresent()) {
                if (LiveManager.isLive(player)) {
                    commandContext.getSender().sendMessage("&eAttempting to set status to offline&8...");

                    if (! LiveManager.initiateOffline(player)) {
                        commandContext.getSender().sendMessage("&cYou have already set your status as offline recently!");
                        return;
                    }

                    return;
                } else {
                    commandContext.getSender().sendMessage("&cYou are already offline.");
                    return;
                }
            } else {
                commandContext.getSender().sendMessage("&cYou must be a streamer to use this command!");

                return;
            }
        }

        setOtherOffline(commandContext);
    }

    public static void setOtherOffline(CommandContext<CosmicCommand> commandContext) {
        CosmicSender sender = commandContext.getSender();
        if (! sender.hasPermission("streamersunite.command.go-offline.others")) {
            commandContext.getSender().sendMessage("&cYou do not have permission to set other players' live status!");
            return;
        }

        if (! commandContext.isArgUsable(0)) {
            commandContext.getSender().sendMessage("&cYou must specify a player!");
            return;
        }

        String player = commandContext.getStringArg(0);

        Optional<CosmicSender> optional = StreamerUtils.getOrGetSenderByName(player);
        if (optional.isEmpty()) {
            commandContext.getSender().sendMessage("&cThat player does not exist!");
            return;
        }

        CosmicSender p = optional.get();

        if (StreamersUnite.getStreamerConfig().getSetup(p.getUuid()).isEmpty()) {
            commandContext.getSender().sendMessage("&cThat player is not a streamer!");
            return;
        }

        if (! LiveManager.isLive(p)) {
            commandContext.getSender().sendMessage("&cThat player is already offline!");
            return;
        }

        commandContext.getSender().sendMessage("&eAttempting to set &f" + p.getCurrentName() + " &eas no longer live&8...");

        if (! LiveManager.initiateOffline(p)) {
            commandContext.getSender().sendMessage("&cThat player has already set their status as offline recently!");
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext commandContext) {
        CosmicSender sender = commandContext.getSender();
        if (! sender.hasPermission("streamersunite.command.go-offline.others")) {
            if (NotificationTimer.hasNotification("go-offline", sender)) {
                return new ConcurrentSkipListSet<>();
            }

            commandContext.getSender().sendMessage("&cYou do not have permission to set other players' live status!");

            NotificationTimer.addNotification("go-offline", sender);
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
