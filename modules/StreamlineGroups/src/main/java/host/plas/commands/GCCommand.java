package host.plas.commands;

import gg.drak.thebase.utils.StringUtils;
import host.plas.StreamlineGroups;
import host.plas.data.GroupManager;
import singularity.command.ModuleCommand;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Sends a single message to the sender's guild, the guild counterpart to
 * {@link PCCommand}.
 */
public class GCCommand extends ModuleCommand {
    private final String errorsNoMessage;

    public GCCommand() {
        super(StreamlineGroups.getInstance(), "gc", "streamline.command.guild.quick-chat", "gchat", "guildchat", "guildc");

        errorsNoMessage = getCommandResource().getOrSetDefault("messages.errors.no-message", "&cYou must provide a message to send!");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings.length > 0) {
            GroupManager.chatGuild(sender, sender, StringUtils.argsToString(strings));
        } else {
            ModuleUtils.sendMessage(sender, errorsNoMessage);
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender sender, String[] strings) {
        return new ConcurrentSkipListSet<>();
    }
}
