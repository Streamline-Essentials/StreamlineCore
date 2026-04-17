package net.streamline.api.base.commands;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Command for viewing and modifying a player's tracked play-time.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code /proxyplaytime <player>} — display the player's current play-time.</li>
 *   <li>{@code /proxyplaytime <player> set <seconds>} — set play-time to the given value.</li>
 *   <li>{@code /proxyplaytime <player> add <seconds>} — add seconds to play-time.</li>
 *   <li>{@code /proxyplaytime <player> remove <seconds>} — subtract seconds from play-time.</li>
 * </ul>
 * Registered under the aliases {@code pplaytime}, {@code pplay}, and {@code proxyplay}.
 */
public class PlaytimeCommand extends CosmicCommand {

    /**
     * Configurable message template displayed when querying a player's play-time.
     * Supports {@code %streamline_user_play_seconds%} and {@code %this_other%}.
     */
    private final String messageGet;

    /**
     * Configurable message template displayed after setting a player's play-time.
     * Supports {@code %this_value%} and {@code %this_other%}.
     */
    private final String messageSet;

    /**
     * Configurable message template displayed after adding seconds to a player's play-time.
     * Supports {@code %this_value%} and {@code %this_other%}.
     */
    private final String messageAdd;

    /**
     * Configurable message template displayed after removing seconds from a player's play-time.
     * Supports {@code %this_value%} and {@code %this_other%}.
     */
    private final String messageRemove;

    /**
     * Registers the playtime command with the {@code streamline-base} module and
     * loads all response message templates from the command resource file.
     */
    public PlaytimeCommand() {
        super(
                "streamline-base",
                "proxyplaytime",
                "streamline.command.playtime.default",
                "pplaytime", "pplay", "proxyplay"
        );

        this.messageGet = this.getCommandResource().getOrSetDefault("messages.playtime.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime&8: " +
                        "&r%streamline_user_play_seconds% &dseconds");
        this.messageSet = this.getCommandResource().getOrSetDefault("messages.playtime.set",
                "&eSet &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime &eto &a%this_value% &dseconds&8!");
        this.messageAdd = this.getCommandResource().getOrSetDefault("messages.playtime.add",
                "&eAdded &a%this_value% &dseconds &eto &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime&8!");
        this.messageRemove = this.getCommandResource().getOrSetDefault("messages.playtime.remove",
                "&eRemoved &a%this_value% &dseconds &efrom &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime&8!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the target {@link singularity.data.players.CosmicPlayer} from
     * argument 0.  With only one argument the player's current play-time is
     * displayed.  With three arguments the second selects the action
     * ({@code set}, {@code add}, or {@code remove}) and the third provides the
     * integer number of seconds to apply.</p>
     *
     * @param context the command context carrying the sender and parsed arguments
     */
    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = context.getStringArg(0);
        CosmicPlayer other = UserUtils.getOrCreatePlayerByName(playerName).orElse(null);

        if (other == null) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (context.getArgCount() == 1) {
            context.sendMessage(getWithOther(context.getSender(), this.messageGet, playerName));
            return;
        }

        String action = context.getStringArg(1);
        int amount = 0;
        try {
            amount = Integer.parseInt(context.getStringArg(2));
        } catch (Exception e) {
            e.printStackTrace();
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set":
                other.setPlaySeconds(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageSet, other));
                break;
            case "add":
                other.addPlaySeconds(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageAdd, other));
                break;
            case "remove":
                other.removePlaySecond(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageRemove, other));
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
     *   <li>Argument 2 — action keywords: {@code set}, {@code add}, {@code remove}.</li>
     *   <li>Argument 3 — integer placeholder via {@link #getIntegerArgument()}.</li>
     * </ol>
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
            return new ConcurrentSkipListSet<>(List.of("set", "add", "remove"));
        }
        if (context.getArgCount() == 3) {
            return getIntegerArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
