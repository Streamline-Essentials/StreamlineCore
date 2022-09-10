package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends StreamlineCommand {
    private final String messageResult;

    public ReloadCommand() {
        super(
                "streamlinereload",
                "streamline.command.streamlinereload.default",
                "slrl", "slreload", "slr"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result",
                "&eReloaded Streamline and modules&8!");
    }

    @Override
    public void run(StreamlineUser sender, String[] args) {
        GivenConfigs.getMainConfig().reloadResource(true);
        GivenConfigs.getMainMessages().reloadResource(true);

        for (StreamlineCommand command : new ArrayList<>(SLAPI.getInstance().getPlatform().getLoadedStreamlineCommands().values())) {
            SLAPI.getInstance().getPlatform().unregisterStreamlineCommand(command);
            command.getCommandResource().reloadResource(true);
            command.getCommandResource().syncCommand();
            SLAPI.getInstance().getPlatform().registerStreamlineCommand(command);
        }

        ModuleManager.restartModules();

        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, messageResult, sender));
    }

    @Override
    public List<String> doTabComplete(StreamlineUser sender, String[] args) {
        return new ArrayList<>();
    }
}