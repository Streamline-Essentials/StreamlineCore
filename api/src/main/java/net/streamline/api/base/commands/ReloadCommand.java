package net.streamline.api.base.commands;

import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.GivenConfigs;
import singularity.modules.ModuleManager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

public class ReloadCommand extends CosmicCommand {
    private final String messageResult;

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

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}