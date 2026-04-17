package singularity.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import singularity.events.server.ServerLogTextEvent;

/**
 * A Logback appender that intercepts log events and fires them as
 * {@link ServerLogTextEvent} within the Singularity event system.
 *
 * <p>Logback log levels are mapped to {@link LogIntent} values as follows:
 * DEBUG → {@link LogIntent#DEBUG}, INFO → {@link LogIntent#INFO},
 * WARN → {@link LogIntent#WARNING}, ERROR → {@link LogIntent#SEVERE},
 * anything else → {@link LogIntent#OTHER}.</p>
 *
 * <p>If the resulting {@link ServerLogTextEvent} is cancelled, the log record
 * is suppressed and not forwarded further.</p>
 */
public class CosmicLogbackAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Converts a Logback {@link ILoggingEvent} into a {@link ServerLogTextEvent}
     * and fires it through the Singularity event bus.
     *
     * <p>The append operation is skipped entirely if the fired event is cancelled.</p>
     *
     * @param eventObject the Logback logging event to process
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        Level level = eventObject.getLevel();
        LogIntent intent = null;
        switch (level.levelStr) {
            case "DEBUG":
                intent = LogIntent.DEBUG;
                break;
            case "INFO":
                intent = LogIntent.INFO;
                break;
            case "WARN":
                intent = LogIntent.WARNING;
                break;
            case "ERROR":
                intent = LogIntent.SEVERE;
                break;
            default:
                intent = LogIntent.OTHER;
                break;
        }

        // Create and fire the event
        ServerLogTextEvent event = new ServerLogTextEvent(eventObject.getFormattedMessage(), intent).fire();
        if (event.isCancelled()) {
            return;
        }
    }
}