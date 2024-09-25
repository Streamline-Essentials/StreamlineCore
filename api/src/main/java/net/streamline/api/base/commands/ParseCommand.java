package net.streamline.api.base.commands;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class ParseCommand extends CosmicCommand {
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
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 2) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = context.getStringArg(0);
        CosmicSender player = UserUtils.getOrCreateSenderByName(playerName).orElse(null);

        if (player == null) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        context.sendMessage(MessageUtils.replaceAllPlayerBungee(context.getSender(),
                getWithOther(context.getSender(), this.messageResult
                        .replace("%this_parsed%", ModuleUtils.replacePlaceholders(player, MessageUtils.argsToStringMinus(context.getArgsArray(), 0)))
                        , player)
        ));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) {
            return Singularity.getInstance().getPlatform().getOnlinePlayerNames();
        }

        return new ConcurrentSkipListSet<>();
    }
}
