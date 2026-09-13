package host.plas.commands;

import host.plas.data.LiveManager;
import host.plas.StreamersUnite;
import host.plas.managers.StreamerUtils;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class CheckLiveCMD extends ModuleCommand {
    public CheckLiveCMD() {
        super(StreamersUnite.getInstance(),
                "live-rn",
                "streamersunite.command.live-rn",
                "lrn", "livern");
    }

    @Override
    public void run(CommandContext<CosmicCommand> commandContext) {
        CosmicSender sender = commandContext.getSender();

        if (commandContext.isArgUsable(0) && sender.hasPermission("streamersunite.command.live-rn.other")) {
            String target = commandContext.getStringArg(0);
            Optional<CosmicSender> optional = StreamerUtils.getOrGetSenderByName(target);
            if (optional.isPresent()) {
                sender = optional.get();
            } else {
                commandContext.sendMessage("&cThat player is not online!");
                return;
            }
        }

        commandContext.sendMessage("&5&m     &r &6&lCURRENTLY LIVE &5&m     &r");

        CosmicSender finalSender = sender;
        LiveManager.getCurrentlyLiveSetups().forEach(setup -> setup.tellStreamLinkCurrentlyLive(finalSender));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext commandContext) {
        ConcurrentSkipListSet<String> strings = new ConcurrentSkipListSet<>();

        if (commandContext.isArgUsable(0)) {
            CosmicSender sender = commandContext.getSender();
            if (sender.hasPermission("streamersunite.command.live-rn.other")) {
                UserUtils.getOnlineSenders().values().forEach(player -> {

                    if (player.getCurrentName() != null &&
                            ! player.getCurrentName().isBlank() &&
                            ! player.getCurrentName().isEmpty())
                        strings.add(player.getCurrentName());
                });
            }
        }

        return strings;
    }
}
