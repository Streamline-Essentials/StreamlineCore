package net.streamline.base.commands;

import net.streamline.api.command.ModuleCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

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
    public void run(SavableUser sender, String[] args) {
        Streamline.getMainConfig().reloadResource(true);
        Streamline.getMainMessages().reloadResource(true);

        for (StreamlineCommand command : new ArrayList<>(Streamline.getLoadedStreamlineCommands().values())) {
            Streamline.unregisterStreamlineCommand(command);
            command.getCommandResource().reloadResource(true);
            command.getCommandResource().syncCommand();
            Streamline.registerStreamlineCommand(command);
        }

        ModuleManager.restartModules();

        MessagingUtils.sendMessage(sender, messageResult);
    }

    @Override
    public List<String> doTabComplete(SavableUser sender, String[] args) {
        return new ArrayList<>();
    }
}
