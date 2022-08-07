package net.streamline.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

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
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 2) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        StreamlinePlayer player = SLAPI.getInstance().getPlatform().getSavedPlayer(playerName);
        SLAPI.getInstance().getMessenger().sendMessage(sender, SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(sender.getUUID(),
                getWithOther(this.messageResult, playerName)
                        .replace("%this_parsed%", SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(player, SLAPI.getInstance().getMessenger().argsToStringMinus(args, 0)))
        ));
    }

    @Override
    public List<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }

        return new ArrayList<>();
    }
}
