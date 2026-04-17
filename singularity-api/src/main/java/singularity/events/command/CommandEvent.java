package singularity.events.command;

import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.events.CosmicEvent;

/**
 * Base event raised whenever a {@link CosmicCommand} is involved in an action.
 *
 * <p>Subclasses specialise this event for specific phases of command processing
 * (e.g. execution, result handling).  The generic parameter {@code C} lets
 * listeners constrain the event to a particular command type without unsafe
 * casts.</p>
 *
 * @param <C> the concrete {@link CosmicCommand} subtype associated with this event
 */
@Getter @Setter
public class CommandEvent<C extends CosmicCommand> extends CosmicEvent {

    /**
     * The command instance that triggered this event.
     */
    private C command;

    /**
     * Constructs a {@code CommandEvent} for the given command.
     *
     * @param command the command that triggered this event; must not be {@code null}
     */
    public CommandEvent(C command) {
        this.command = command;
    }
}
