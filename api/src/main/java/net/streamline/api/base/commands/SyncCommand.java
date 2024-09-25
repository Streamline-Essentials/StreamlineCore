package net.streamline.api.base.commands;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class SyncCommand extends CosmicCommand {
    private final String messageResult;
    private final String messageErrorNot;

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

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (Singularity.getMainDatabase() == null) {
            context.sendMessage(this.messageErrorNot);
            return;
        }

        UserUtils.syncAllUsers();
        context.sendMessage(this.messageResult);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}