package singularity.events.server;

import lombok.Getter;
import singularity.events.CosmicEvent;
import singularity.logging.LogIntent;

/**
 * Represents a single line of text written to the server log.
 *
 * <p>This event is fired whenever a log message is produced by the server or
 * the Singularity framework. It carries both the raw message string and a
 * {@link LogIntent} that describes the severity or purpose of the log entry
 * (e.g. info, warning, error).</p>
 *
 * <p>Multiple instances may be collected and dispatched together via a
 * {@link CosmicLogPopEvent}.</p>
 */
@Getter
public class ServerLogTextEvent extends CosmicEvent {

    /**
     * The raw text content of this log entry.
     */
    private final String message;

    /**
     * The intent (severity/category) of this log entry, indicating how it
     * should be treated or displayed.
     */
    private final LogIntent intent;

    /**
     * Constructs a new {@code ServerLogTextEvent}.
     *
     * @param message the log message text; must not be {@code null}
     * @param intent  the {@link LogIntent} describing the nature of the log
     *                entry; must not be {@code null}
     */
    public ServerLogTextEvent(String message, LogIntent intent) {
        this.message = message;
        this.intent = intent;
    }
}
