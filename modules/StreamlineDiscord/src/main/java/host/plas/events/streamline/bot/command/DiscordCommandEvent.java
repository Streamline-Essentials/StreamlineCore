package host.plas.events.streamline.bot.command;

import lombok.Getter;
import lombok.Setter;
import host.plas.discord.DiscordCommand;
import host.plas.discord.MessagedString;
import host.plas.events.streamline.bot.posting.DiscordMessageEvent;

@Setter
@Getter
public class DiscordCommandEvent extends DiscordMessageEvent {
    private DiscordCommand command;

    public DiscordCommandEvent(MessagedString message, DiscordCommand command) {
        super(message);
        setCommand(command);
    }
}
