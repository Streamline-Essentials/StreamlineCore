package net.streamline.base.commands;

import net.streamline.api.command.ICommandSender;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand extends StreamlineCommand {
    private String messageResult;

    public ReloadCommand() {
        super(
                "streamlinereload",
                "A command to reload everything Streamline!",
                "/streamlinereload",
                "streamline.command.streamlinereload.default",
                "slrl", "slreload", "slr"
        );

        this.messageResult = this.commandResource.getOrSetDefault("messages.result",
                "&eReloaded Streamline and modules&8!");
    }

    @Override
    public void run(ICommandSender sender, String[] args) {
        Streamline.getMainConfig().reloadResource(true);
        Streamline.getMainMessages().reloadResource(true);
        Streamline.flushCommands();

        for (StreamlineCommand command : Streamline.getLoadedCommands().values()) {
            command.commandResource.reloadResource(true);
            command.commandResource.syncCommand();
            Streamline.registerCommand(command);
        }

        for (ModuleCommand command : Streamline.getLoadedModuleCommands().values()) {
            command.
        }
    }

    @Override
    public List<String> doTabComplete(ICommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
