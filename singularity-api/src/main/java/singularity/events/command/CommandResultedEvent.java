package singularity.events.command;

import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;

@Getter @Setter
public class CommandResultedEvent<C extends CosmicCommand> extends CommandEvent<C> {
    private CommandContext<C> context;

    public CommandResultedEvent(CommandContext<C> context) {
        super(context.getCommand());
        this.context = context;
    }
}
