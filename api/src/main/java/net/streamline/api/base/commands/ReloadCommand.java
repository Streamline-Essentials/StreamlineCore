package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.CommandHandler;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.modules.ModuleManager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

public class ReloadCommand extends StreamlineCommand {
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
    public void run(StreamSender sender, String[] args) {
        GivenConfigs.getMainConfig().reloadResource(true);
        GivenConfigs.getMainMessages().reloadResource(true);

        for (StreamlineCommand command : new ArrayList<>(CommandHandler.getLoadedStreamlineCommands().values())) {
            CommandHandler.unregisterStreamlineCommand(command);
            command.getCommandResource().reloadResource(true);
            command.getCommandResource().syncCommand();
            CommandHandler.registerStreamlineCommand(command);
        }

        ModuleManager.restartModules();

        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, messageResult, sender));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamSender sender, String[] args) {
        return new ConcurrentSkipListSet<>();
    }
}