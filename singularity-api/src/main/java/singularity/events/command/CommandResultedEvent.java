package singularity.events.command;

import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;

/**
 * Fired after a {@link CosmicCommand} has been executed and a result is available.
 *
 * <p>In addition to the command itself (inherited from {@link CommandEvent}), this
 * event carries the full {@link CommandContext} that was produced during execution,
 * allowing listeners to inspect arguments, the sender, and any outcome state.</p>
 *
 * @param <C> the concrete {@link CosmicCommand} subtype associated with this event
 */
@Getter @Setter
public class CommandResultedEvent<C extends CosmicCommand> extends CommandEvent<C> {

    /**
     * The context produced by executing the command, including the sender,
     * parsed arguments, and execution result.
     */
    private CommandContext<C> context;

    /**
     * Constructs a {@code CommandResultedEvent} from the given execution context.
     *
     * <p>The command stored on this event is taken directly from
     * {@link CommandContext#getCommand()}, so callers only need to supply the
     * context.</p>
     *
     * @param context the execution context returned after the command ran;
     *                must not be {@code null}
     */
    public CommandResultedEvent(CommandContext<C> context) {
        super(context.getCommand());
        this.context = context;
    }
}
