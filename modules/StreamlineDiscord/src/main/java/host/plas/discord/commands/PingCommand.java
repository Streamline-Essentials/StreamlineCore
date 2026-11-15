package host.plas.discord.commands;

import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import singularity.objects.SingleSet;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;

public class PingCommand extends DiscordCommand {
    public PingCommand() {
        super("ping",
                "ping", "p"
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
        return respond("commands.ping.response", messagedString);
    }
}
