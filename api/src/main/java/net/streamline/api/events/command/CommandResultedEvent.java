package net.streamline.api.events.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.context.CommandContext;

@Getter @Setter
public class CommandResultedEvent<C extends StreamlineCommand> extends CommandEvent<C> {
    private CommandContext<C> context;

    public CommandResultedEvent(CommandContext<C> context) {
        super(context.getCommand());
        this.context = context;
    }
}
