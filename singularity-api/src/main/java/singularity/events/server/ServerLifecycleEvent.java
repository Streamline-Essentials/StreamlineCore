package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;

/**
 * Base event for server lifecycle transitions such as startup and shutdown.
 *
 * <p>Carries an optional human-readable message that describes or annotates
 * the lifecycle change. The message may be built incrementally via
 * {@link #appendLine(String)} and {@link #append(String)}, and converted to
 * or from a {@link StringBuilder} for more complex string construction.</p>
 *
 * <p>Subclasses such as {@link ServerStartEvent} and {@link ServerStopEvent}
 * represent specific lifecycle points.</p>
 */
@Setter
@Getter
public class ServerLifecycleEvent extends CosmicEvent {

    /**
     * An optional message associated with this lifecycle event. May be
     * {@code null} when no message has been set, in which case
     * {@link #isSendable()} returns {@code false}.
     */
    private String message;

    /**
     * Constructs a new {@code ServerLifecycleEvent} with the given message.
     *
     * @param message the initial message for this event, or {@code null} if
     *                no message is required
     */
    public ServerLifecycleEvent(String message) {
        setMessage(message);
    }

    /**
     * Constructs a new {@code ServerLifecycleEvent} with no message.
     * {@link #isSendable()} will return {@code false} until a message is set.
     */
    public ServerLifecycleEvent() {
        this(null);
    }

    /**
     * Returns {@code true} if this event has a non-{@code null} message and
     * may therefore be dispatched or displayed to recipients.
     *
     * @return {@code true} when the message is not {@code null}
     */
    public boolean isSendable() {
        return getMessage() != null;
    }

    /**
     * Appends a new line to the current message, separated by the
     * {@code %newline%} token.
     *
     * @param line the text to append as a new line; must not be {@code null}
     */
    public void appendLine(String line) {
        setMessage(getMessage() + "%newline%" + line);
    }

    /**
     * Appends the given string directly to the current message without any
     * separator.
     *
     * @param toAppend the text to append; must not be {@code null}
     */
    public void append(String toAppend) {
        setMessage(getMessage() + toAppend);
    }

    /**
     * Returns the current message wrapped in a new {@link StringBuilder},
     * suitable for further manipulation.
     *
     * @return a {@link StringBuilder} containing the current message
     */
    public StringBuilder asStringBuilder() {
        return new StringBuilder(getMessage());
    }

    /**
     * Replaces the current message with the contents of the given
     * {@link StringBuilder} and returns the resulting string.
     *
     * @param builder the {@link StringBuilder} whose content should become the
     *                new message; must not be {@code null}
     * @return the updated message string
     */
    public String fromStringBuilder(StringBuilder builder) {
        setMessage(builder.toString());
        return getMessage();
    }
}
