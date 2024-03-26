package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.context.CommandContext;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class SyncCommand extends StreamlineCommand {
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
    public void run(CommandContext<StreamlineCommand> context) {
        if (SLAPI.getMainDatabase() == null) {
            context.sendMessage(this.messageErrorNot);
            return;
        }

        UserUtils.syncAllUsers();
        context.sendMessage(this.messageResult);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<StreamlineCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}