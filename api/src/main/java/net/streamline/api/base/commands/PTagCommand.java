package net.streamline.api.base.commands;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Command for managing metadata tags on a {@link singularity.data.players.CosmicPlayer}.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code /ptag <player>} — list the player's current tags.</li>
 *   <li>{@code /ptag <player> add <tag...>} — add one or more tags to the player.</li>
 *   <li>{@code /ptag <player> remove <tag...>} — remove one or more tags from the player.</li>
 * </ul>
 * Registered under the alias {@code proxytag}.
 */
public class PTagCommand extends CosmicCommand {

    /**
     * Configurable message template displayed when listing a player's tags.
     * Supports {@code %streamline_user_tags%} and {@code %this_other%}.
     */
    private final String messageTagsGet;

    /**
     * Configurable message template displayed after adding a tag.
     * Supports {@code %this_value%} and {@code %this_other%}.
     */
    private final String messageTagsAdd;

    /**
     * Configurable message template displayed after removing a tag.
     * Supports {@code %this_value%} and {@code %this_other%}.
     */
    private final String messageTagsRemove;

    /**
     * Registers the ptag command with the {@code streamline-base} module and
     * loads all response message templates from the command resource file.
     */
    public PTagCommand() {
        super(
                "streamline-base",
                "ptag",
                "streamline.command.tag.default",
                "proxytag"
        );

        this.messageTagsGet = this.getCommandResource().getOrSetDefault("messages.tags.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &ctags&8: " +
                        "&r%streamline_user_tags%");
        this.messageTagsAdd = this.getCommandResource().getOrSetDefault("messages.tags.add",
                "&eAdded &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &ctag&8: " +
                        "&a%this_value%");
        this.messageTagsRemove = this.getCommandResource().getOrSetDefault("messages.tags.remove",
                "&eRemoved &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &ctag&8: " +
                        "&a%this_value%");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the target player from argument 0.  With one argument the
     * player's tag list is displayed.  With three or more arguments the second
     * argument selects either {@code add} or {@code remove}, and all subsequent
     * tokens are treated as individual tag values to apply.</p>
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
        CosmicPlayer other = UserUtils.getOrCreatePlayerByName(playerName).orElse(null);

        if (other == null) {
            ModuleUtils.sendMessage(context.getSender(), MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (context.getArgCount() == 1) {
            context.sendMessage(getWithOther(context.getSender(), this.messageTagsGet, playerName));
            return;
        }

        if (context.getArgCount() < 3) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = context.getStringArg(1);
        String[] actions = MessageUtils.argsToStringMinus(context.getArgsArray(), 0, 1).split(" ");

        switch (action) {
            case "add":
                Arrays.stream(actions).forEach(other::addTag);
                context.sendMessage(getWithOther(context.getSender(), this.messageTagsAdd, other));
                break;
            case "remove":
                Arrays.stream(actions).forEach(other::removeTag);
                context.sendMessage(getWithOther(context.getSender(), this.messageTagsRemove, other));
                break;
            default:
                context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tab-completion progression:
     * <ol>
     *   <li>Argument 1 — online player names.</li>
     *   <li>Argument 2 — action keywords: {@code add}, {@code remove}.</li>
     * </ol>
     * No completions are offered for tag values.
     *
     * @param context the command context carrying the sender and current argument list
     * @return a sorted set of tab-completion candidates
     */
    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) {
            return Singularity.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (context.getArgCount() == 2) {
            return new ConcurrentSkipListSet<>(List.of("add", "remove"));
        }

        return new ConcurrentSkipListSet<>();
    }
}
