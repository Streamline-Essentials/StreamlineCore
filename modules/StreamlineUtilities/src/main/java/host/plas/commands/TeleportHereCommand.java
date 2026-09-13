package host.plas.commands;

import host.plas.StreamlineUtilities;
import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class TeleportHereCommand extends ModuleCommand {
    private final String messageResult;

    public TeleportHereCommand() {
        super(StreamlineUtilities.getInstance(),
                "pteleporthere",
                "streamline.command.teleporthere.default",
                "ptelehere", "ptphere"
        );

        messageResult = getCommandResource().getOrSetDefault("messages.result", "&eTeleported %streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &eto &dyou&8!");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (! (sender instanceof CosmicPlayer)) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        }
        CosmicPlayer player = (CosmicPlayer) sender;

        if (strings[0].isEmpty()) {
            ModuleUtils.sendMessage(player, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        } else if (strings.length > 1) {
            ModuleUtils.sendMessage(player, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
            return;
        }

        String toUser = strings[0];
        CosmicPlayer other = UserUtils.getOrCreatePlayerByName(toUser).orElse(null);
        if (other == null) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        other.teleport(player);

        ModuleUtils.sendMessage(sender, getWithOther(sender, messageResult, other));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        return ModuleUtils.getOnlinePlayerNames();
    }
}
