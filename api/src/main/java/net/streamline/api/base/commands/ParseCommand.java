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

/**
 * Command that evaluates placeholder expressions in the context of a given player.
 *
 * <p>Usage: {@code /parse <player> <expression...>}. The supplied expression is
 * processed through the RAT placeholder engine relative to the resolved
 * {@link singularity.data.console.CosmicSender}, and the result is sent back to
 * the command sender. Registered under the aliases {@code parse}, {@code par},
 * and {@code rat-parse}.</p>
 */
public class ParseCommand extends CosmicCommand {

    /**
     * The configurable message template used to present the parse result to the
     * sender. Supports placeholders such as {@code %this_parsed%} and
     * {@code %this_other%}.
     */
    private final String messageResult;

    /**
     * Registers the parse command with the {@code streamline-base} module and
     * loads the result message template from the command resource file.
     */
    public ParseCommand() {
        super(
                "streamline-base",
                "parse",
                "streamline.command.parse.default",
                "par", "rat-parse"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result", "&eRan parser on &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8: &r%this_parsed%");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the target player from argument 0, replaces all placeholders
     * in the remaining arguments using that player's context, and sends the
     * formatted result to the command sender.</p>
     *
     * @param context the command context carrying the sender and parsed arguments
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Suggests online player names for the first argument position. No
     * completions are offered for subsequent arguments.</p>
     *
     * @param context the command context carrying the sender and current argument list
     * @return a sorted set of online player names, or an empty set for later arguments
     */
    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) {
            return Singularity.getInstance().getPlatform().getOnlinePlayerNames();
        }

        return new ConcurrentSkipListSet<>();
    }
}
