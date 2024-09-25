package singularity.events.command;

import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.events.CosmicEvent;

@Getter @Setter
public class CommandEvent<C extends CosmicCommand> extends CosmicEvent {
    private C command;

    public CommandEvent(C command) {
        this.command = command;
    }
}
