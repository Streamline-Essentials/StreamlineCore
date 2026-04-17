package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

/**
 * Fired when a {@link CosmicPlayer} sends a chat message.
 *
 * <p>The event stores both the original, unmodified message text and a mutable
 * copy that listeners may alter before it is delivered.  It also exposes a
 * cancellation flag so listeners can suppress the message entirely.</p>
 */
@Getter
public class CosmicChatEvent extends CosmicSenderEvent {

    /**
     * The raw message text as submitted by the player, before any listener
     * modification.  This value never changes after event construction.
     */
    private final String originalMessage;

    /**
     * The message text that will actually be delivered.  Listeners may replace
     * this value to modify what other players see.
     */
    @Setter
    private String message;

    /**
     * Whether this chat message has been cancelled.  When {@code true}, the
     * message should not be broadcast to other players.
     */
    @Setter
    private boolean canceled;

    /**
     * Constructs a {@code CosmicChatEvent} for the given player and message.
     *
     * <p>Both {@link #originalMessage} and {@link #message} are initialised to
     * {@code message}, and {@link #canceled} defaults to {@code false}.</p>
     *
     * @param sender  the player who sent the message; must not be {@code null}
     * @param message the raw chat message text; must not be {@code null}
     */
    public CosmicChatEvent(CosmicPlayer sender, String message) {
        super(sender);
        this.originalMessage = message;
        this.message = message;
        this.canceled = false;
    }
}
