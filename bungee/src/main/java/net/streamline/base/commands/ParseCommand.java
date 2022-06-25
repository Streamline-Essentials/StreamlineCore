package net.streamline.base.commands;

import net.streamline.api.command.ICommandSender;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class ParseCommand extends StreamlineCommand {
    private String messageResult;

    public ParseCommand() {
        super(
                "parse",
                "A command to parse thing in-game with the RAT API!",
                "/parse <player> <things to parse>",
                "streamline.command.parse.default",
                Streamline.getMainCommandsFolder(),
                "par", "rat-parse"
        );

        this.messageResult = this.commandResource.getOrSetDefault("messages.result", "&eRan parser on &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8: &r%this_parsed%");
    }

    @Override
    public void run(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        SavablePlayer player = Streamline.getInstance().getSavedPlayer(playerName);
        MessagingUtils.sendMessage(sender, MessagingUtils.replaceAllPlayerBungee(sender.getUUID(),
                this.messageResult
                        .replace("%this_other%", playerName)
                        .replace("%this_parsed%", MessagingUtils.replaceAllPlayerBungee(player, MessagingUtils.argsToStringMinus(args, 0)))
        ));
    }

    @Override
    public List<String> doTabComplete(ICommandSender sender, String[] args) {
        if (args.length <= 1) {
            return Streamline.getInstance().getOnlinePlayerNames();
        }

        return new ArrayList<>();
    }
}
