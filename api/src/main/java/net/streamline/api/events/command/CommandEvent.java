package net.streamline.api.events.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.events.StreamlineEvent;

@Getter @Setter
public class CommandEvent<C extends StreamlineCommand> extends StreamlineEvent {
    private C command;

    public CommandEvent(C command) {
        this.command = command;
    }
}
