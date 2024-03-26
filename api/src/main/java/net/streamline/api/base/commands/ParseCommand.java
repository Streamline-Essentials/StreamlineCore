package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.context.CommandContext;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class ParseCommand extends StreamlineCommand {
    private final String messageResult;

    public ParseCommand() {
        super(
                "streamline-base",
                "parse",
                "streamline.command.parse.default",
                "par", "rat-parse"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result", "&eRan parser on &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8: &r%this_parsed%");
    }

    @Override
    public void run(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() < 2) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = context.getStringArg(0);
        StreamSender player = UserUtils.getOrCreateSenderByName(playerName).orElse(null);

        if (player == null) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        context.sendMessage(MessageUtils.replaceAllPlayerBungee(context.getSender(),
                getWithOther(context.getSender(), this.messageResult
                        .replace("%this_parsed%", MessageUtils.replaceAllPlayerBungee(player, MessageUtils.argsToStringMinus(context.getArgsArray(), 0)))
                        , player)
        ));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }

        return new ConcurrentSkipListSet<>();
    }
}
