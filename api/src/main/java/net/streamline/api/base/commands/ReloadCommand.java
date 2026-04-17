package net.streamline.api.base.commands;

import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.GivenConfigs;
import singularity.modules.ModuleManager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Command that hot-reloads Streamline's configuration and all loaded modules.
 *
 * <p>On execution this command:
 * <ol>
 *   <li>Reloads the main config and main messages resource files from disk.</li>
 *   <li>Unregisters every loaded Streamline command, reloads its resource, syncs
 *       it, and re-registers it.</li>
 *   <li>Restarts all PF4J modules via {@link singularity.modules.ModuleManager}.</li>
 * </ol>
 * Registered under the aliases {@code slrl}, {@code slreload}, and {@code slr}.
 */
public class ReloadCommand extends CosmicCommand {

    /** Configurable feedback message sent to the sender upon successful reload. */
    private final String messageResult;

    /**
     * Registers the reload command with the {@code streamline-base} module and
     * loads the result message template from the command resource file.
     */
    public ReloadCommand() {
        super(
                "streamline-base",
                "streamlinereload",
                "streamline.command.streamlinereload.default",
                "slrl", "slreload", "slr"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result",
                "&eReloaded Streamline and modules&8!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reloads configuration resources, synchronises all registered Streamline
     * commands with their (possibly updated) resource files, and restarts every
     * loaded module before notifying the sender.</p>
     *
     * @param context the command context carrying the sender and parsed arguments
     */
    @Override
    public void run(CommandContext<CosmicCommand> context) {
        GivenConfigs.getMainConfig().reloadResource(true);
        GivenConfigs.getMainMessages().reloadResource(true);

        for (CosmicCommand command : new ArrayList<>(CommandHandler.getLoadedStreamlineCommands().values())) {
            CommandHandler.unregisterStreamlineCommand(command);
            command.getCommandResource().reloadResource(true);
            command.getCommandResource().syncCommand();
            CommandHandler.registerStreamlineCommand(command);
        }

        ModuleManager.restartModules();

        context.sendMessage(getWithOther(context.getSender(), messageResult, context.getSender()));
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