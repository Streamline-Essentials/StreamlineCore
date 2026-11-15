package host.plas.discord.commands;

import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;
import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordCommand;
import host.plas.discord.DiscordHandler;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;

public class RestartCommand extends DiscordCommand {
    public RestartCommand() {
        super("restart",
                -1L,
                "restart", "res"
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
        if (! DiscordHandler.init().join()) {
            StreamlineDiscord.getInstance().logWarning("Could not start Discord Module properly... (Timed out!)");
        }

        return respond("commands.restart.response", messagedString);
    }
}
