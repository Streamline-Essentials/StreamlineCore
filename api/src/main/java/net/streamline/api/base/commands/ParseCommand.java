package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

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
        StreamlineUser player = UserUtils.getOrGetUserByName(playerName);

        if (player == null) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        SLAPI.getInstance().getMessenger().sendMessage(sender, MessageUtils.replaceAllPlayerBungee(sender,
                getWithOther(sender, this.messageResult, player)
                        .replace("%this_parsed%", MessageUtils.replaceAllPlayerBungee(player, MessageUtils.argsToStringMinus(args, 0)))
        ));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }

        return new ConcurrentSkipListSet<>();
    }
}
