package net.streamline.api.base.commands;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Command that forces an immediate database sync of all loaded user data.
 *
 * <p>Calls {@link singularity.utils.UserUtils#syncAllUsers()} to flush the
 * in-memory user state to the configured database backend.  If no database
 * backend is configured the command reports an error instead. Registered under
 * the alias {@code slsync}.</p>
 */
public class SyncCommand extends CosmicCommand {

    /** Configurable feedback message sent to the sender after a successful sync. */
    private final String messageResult;

    /**
     * Configurable error message sent to the sender when no database backend is
     * available and the sync cannot proceed.
     */
    private final String messageErrorNot;

    /**
     * Registers the sync command with the {@code streamline-base} module and
     * loads both message templates from the command resource file.
     */
    public SyncCommand() {
        super(
                "streamline-base",
                "streamlinesync",
                "streamline.command.streamlinesync.default",
                "slsync"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result",
                "&eReloaded Streamline and modules&8!");
        this.messageErrorNot = this.getCommandResource().getOrSetDefault("messages.error.not-syncable",
                "&cSave type is not a database!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Aborts with an error message when no main database is configured;
     * otherwise triggers {@link singularity.utils.UserUtils#syncAllUsers()} and
     * notifies the sender of success.</p>
     *
     * @param context the command context carrying the sender and parsed arguments
     */
    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (Singularity.getMainDatabase() == null) {
            context.sendMessage(this.messageErrorNot);
            return;
        }

        UserUtils.syncAllUsers();
        context.sendMessage(this.messageResult);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This command accepts no arguments; an empty set is always returned.</p>
     *
     * @param context the command context carrying the sender and current argument list
     * @return an empty set — no tab-completions available
     */
    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}