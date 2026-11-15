package host.plas.discord.commands;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.discord.messaging.BotMessageConfig;
import singularity.objects.SingleSet;

@Setter
@Getter
public class HelpCommand extends DiscordCommand {
    public HelpCommand() {
        super("help",
                "help", "h"
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
        return respond("commands.help.response", messagedString);
    }
}
