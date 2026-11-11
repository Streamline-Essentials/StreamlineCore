package host.plas.discord.commands;

import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.command.CommandHandler;
import singularity.configs.given.GivenConfigs;
import singularity.modules.ModuleManager;
import singularity.objects.SingleSet;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;

public class ReloadCommand extends DiscordCommand {
    public ReloadCommand() {
        super("reload",
                -1L,
                "reload", "rel"
        );
    }

    @Override
    public void init() {

    }

    @Override
    public CommandCreateAction setupOptionData(CommandCreateAction action) {
        return action;
    }

    @Override
    public SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString) {
        GivenConfigs.getMainConfig().reloadResource(true);
        GivenConfigs.getMainMessages().reloadResource(true);

        CommandHandler.getLoadedStreamlineCommands().forEach((s, command) -> {
            CommandHandler.unregisterStreamlineCommand(command);
            command.getCommandResource().reloadResource(true);
            command.getCommandResource().syncCommand();
            CommandHandler.registerStreamlineCommand(command);
        });

        ModuleManager.restartModules();

        return respond("commands.reload.response", messagedString);
    }
}
