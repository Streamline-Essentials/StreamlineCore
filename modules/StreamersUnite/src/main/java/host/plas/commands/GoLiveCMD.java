package host.plas.commands;

import host.plas.StreamersUnite;
import host.plas.data.LiveManager;
import host.plas.managers.NotificationTimer;
import host.plas.managers.StreamerUtils;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class GoLiveCMD extends ModuleCommand {
    public GoLiveCMD() {
        super(StreamersUnite.getInstance(),
                "go-live",
                "streamersunite.command.go-live",
                "glive", "live", "golive");
    }

    @Override
    public void run(CommandContext<CosmicCommand> commandContext) {
        if (commandContext.isConsole()) {
            other(commandContext);
            return;
        }

        CosmicSender player = commandContext.getSender();

        if (! commandContext.isArgUsable(0)) {
            if (StreamersUnite.getStreamerConfig().getSetup(player.getUuid()).isPresent()) {
                if (LiveManager.isLive(player)) {
                    commandContext.getSender().sendMessage("&cYou are already live.");
                    return;
                } else {
                    commandContext.getSender().sendMessage("&eAttempting to set status to live&8...");

                    if (! LiveManager.initiateLive(player)) {
                        commandContext.getSender().sendMessage("&cYou have already set your status as live recently!");
                        return;
                    }

                    return;
                }
            } else {
                commandContext.getSender().sendMessage("&cYou must be a streamer to use this command!");

                return;
            }
        }

        other(commandContext);
    }

    public static void other(CommandContext commandContext) {
        CosmicSender sender = commandContext.getSender();
        if (! sender.hasPermission("streamersunite.command.go-live.others")) {
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

        if (LiveManager.isLive(p)) {
            commandContext.getSender().sendMessage("&cThat player is already live!");
            return;
        }

        commandContext.getSender().sendMessage("&eAttempting to set &r" + p.getCurrentName() + " &eas live&8...");

        if (! LiveManager.initiateLive(p)) {
            commandContext.getSender().sendMessage("&cThat player has already set their status as live recently!");
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext commandContext) {
        CosmicSender sender = commandContext.getSender();
        if (! sender.hasPermission("streamersunite.command.go-live.others")) {
            if (NotificationTimer.hasNotification("go-live", sender)) {
                return new ConcurrentSkipListSet<>();
            }

            commandContext.getSender().sendMessage("&cYou do not have permission to set other players' live status to live!");

            NotificationTimer.addNotification("go-live", sender);
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
