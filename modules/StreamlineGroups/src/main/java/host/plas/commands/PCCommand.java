package host.plas.commands;

import gg.drak.thebase.utils.StringUtils;
import singularity.command.ModuleCommand;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineGroups;
import host.plas.data.GroupManager;

import java.util.concurrent.ConcurrentSkipListSet;

public class PCCommand extends ModuleCommand {
    String errorsNoMessage;

    public PCCommand() {
        super(StreamlineGroups.getInstance(), "pc", "streamline.command.party.quick-chat", "pchat", "partychat", "partyc");

        errorsNoMessage = getCommandResource().getOrSetDefault("messages.errors.no-message", "&cYou must provide a message to send!");
    }

    @Override
    public void run(CosmicSender CosmicSender, String[] strings) {
        if (strings.length > 0) {
            String message = StringUtils.argsToString(strings);
            GroupManager.chatParty(CosmicSender, CosmicSender, message);
        } else {
            ModuleUtils.sendMessage(CosmicSender, errorsNoMessage);
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        return new ConcurrentSkipListSet<>();
    }
}
