package host.plas.commands;

import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import host.plas.StreamlineUtilities;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class BroadcastCommand extends ModuleCommand {
    private final String messageResultAll;

    public BroadcastCommand() {
        super(StreamlineUtilities.getInstance(),
                "proxybroadcast",
                "streamline.command.broadcast.default",
                "pb", "proxyb", "pbroadcast"
        );

        messageResultAll = getCommandResource().getOrSetDefault("messages.result.all", "&c&lBROADCAST &7&l>> &r%this_message%");
    }

    @Override
    public void run(CosmicSender CosmicSender, String[] strings) {
        String message = ModuleUtils.argsToString(strings);

        boolean isCustom = false;
        if (message.startsWith("!")) {
            isCustom = true;
            message = message.substring(1);
        }

        final String finalMessage = message;
        boolean finalIsCustom = isCustom;
        ModuleUtils.getLoadedSendersSet().forEach(a -> {
            if (! finalIsCustom) {
                ModuleUtils.sendMessage(a, messageResultAll
                        .replace("%this_message%", finalMessage)
                        .replace("%this_sender%", CosmicSender.getCurrentName())
                );
            } else {
                ModuleUtils.sendMessage(a, finalMessage
                        .replace("%this_sender%", CosmicSender.getCurrentName())
                );
            }
        });
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        return new ConcurrentSkipListSet<>();
    }
}
