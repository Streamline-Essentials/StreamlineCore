package net.streamline.api.base.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Command that persists a new identifier for the current server.
 *
 * <p>Usage: {@code /setserveridentifier <name>}. The supplied identifier is
 * written to the Streamline configuration via
 * {@link singularity.configs.given.GivenConfigs#writeServerName(String)} so
 * that it is used as the server name in cross-server messaging and placeholder
 * resolution. Registered under the aliases {@code setidentifier} and
 * {@code setserver}.</p>
 */
public class SetServerCommand extends CosmicCommand {

    /**
     * Configurable feedback message sent to the sender when the identifier is
     * saved successfully. Supports the {@code %this_input%} token.
     */
    private final String messageResultSet;

    /**
     * Configurable error message sent to the sender when the supplied identifier
     * is blank or otherwise invalid.
     */
    private final String messageResultInvalid;

    /**
     * Registers the set-server command with the {@code streamline-base} module and
     * loads both response message templates from the command resource file.
     */
    public SetServerCommand() {
        super(
                "streamline-base",
                "setserveridentifier",
                "streamline.command.setserveridentifier.default",
                "setidentifier", "setserver"
        );

        this.messageResultSet = this.getCommandResource().getOrSetDefault("messages.result.set",
                "&eSuccessfully set this server's identifier to &7\"&c%this_input%&7\"&8!");
        this.messageResultInvalid = this.getCommandResource().getOrSetDefault("messages.result.invalid",
                "&cInvalid input. Please provide a valid identifier.&8!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates that exactly one non-blank argument was supplied, then
     * persists the identifier by delegating to
     * {@link singularity.configs.given.GivenConfigs#writeServerName(String)}.</p>
     *
     * @param context the command context carrying the sender and parsed arguments
     */
    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }
        if (context.getArgCount() > 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
            return;
        }

        String input = context.getStringArg(0);
        if (input.isBlank()) {
            context.sendMessage(this.messageResultInvalid);
            return;
        }

        GivenConfigs.writeServerName(input);

        context.sendMessage(this.messageResultSet.replace("%this_input%", input));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Offers the literal placeholder hint {@code <server-name>} when the
     * sender has not yet typed any argument.</p>
     *
     * @param context the command context carrying the sender and current argument list
     * @return a set containing {@code <server-name>} for argument 1, or an empty set otherwise
     */
    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> completions = new ConcurrentSkipListSet<>();

        if (context.getArgCount() == 1) {
            completions.add("<server-name>");
        }

        return completions;
    }
}
