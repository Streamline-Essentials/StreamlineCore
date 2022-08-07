package net.streamline.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.base.Streamline;

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
        SLAPI.getInstance().getPlatform().getMainConfig().reloadResource(true);
        SLAPI.getInstance().getPlatform().getMainMessages().reloadResource(true);

        for (StreamlineCommand command : new ArrayList<>(Streamline.getInstance().getLoadedStreamlineCommands().values())) {
            SLAPI.getInstance().getPlatform().unregisterStreamlineCommand(command);
            command.getCommandResource().reloadResource(true);
            command.getCommandResource().syncCommand();
            SLAPI.getInstance().getPlatform().registerStreamlineCommand(command);
        }

        ModuleManager.restartModules();

        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResult);
    }

    @Override
    public List<String> doTabComplete(StreamlineUser sender, String[] args) {
        return new ArrayList<>();
    }
}
