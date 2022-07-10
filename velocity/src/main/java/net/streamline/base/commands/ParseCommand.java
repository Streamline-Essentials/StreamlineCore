package net.streamline.base.commands;

import net.streamline.api.BasePlugin;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class ParseCommand extends StreamlineCommand {
    private final String messageResult;

    public ParseCommand() {
        super(
                "parse",
                "streamline.command.parse.default",
                "par", "rat-parse"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result", "&eRan parser on &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8: &r%this_parsed%");
    }

    @Override
    public void run(SavableUser sender, String[] args) {
        if (args.length < 2) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        SavablePlayer player = BasePlugin.getSavedPlayer(playerName);
        MessagingUtils.sendMessage(sender, MessagingUtils.replaceAllPlayerBungee(sender.uuid,
                getWithOther(this.messageResult, playerName)
                        .replace("%this_parsed%", MessagingUtils.replaceAllPlayerBungee(player, MessagingUtils.argsToStringMinus(args, 0)))
        ));
    }

    @Override
    public List<String> doTabComplete(SavableUser sender, String[] args) {
        if (args.length <= 1) {
            return BasePlugin.getOnlinePlayerNames();
        }

        return new ArrayList<>();
    }
}
