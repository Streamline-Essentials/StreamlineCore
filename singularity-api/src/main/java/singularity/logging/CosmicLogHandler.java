package singularity.logging;

import singularity.events.server.ServerLogTextEvent;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.logging.Level;

/**
 * A {@link StreamHandler} implementation that bridges the Java Util Logging (JUL)
 * framework into the Singularity event system.
 *
 * <p>JUL log levels are mapped to {@link LogIntent} values as follows:
 * FINE → {@link LogIntent#DEBUG}, INFO → {@link LogIntent#INFO},
 * WARNING → {@link LogIntent#WARNING}, SEVERE → {@link LogIntent#SEVERE},
 * anything else → {@link LogIntent#OTHER}.</p>
 *
 * <p>If the resulting {@link ServerLogTextEvent} is cancelled, the log record
 * is suppressed and not forwarded to the underlying output stream.</p>
 */
public class CosmicLogHandler extends StreamHandler {

    /**
     * Creates a new {@code CosmicLogHandler} and configures it to write to
     * {@link System#out} by default.
     */
    public CosmicLogHandler() {
        // Set output to System.out by default
        super.setOutputStream(System.out);
    }

    /**
     * Intercepts a JUL {@link LogRecord}, maps its level to a {@link LogIntent},
     * and fires a {@link ServerLogTextEvent}. If the event is cancelled the record
     * is not forwarded downstream.
     *
     * @param record the log record to publish
     */
    @Override
    public synchronized void publish(LogRecord record) {
        Level level = record.getLevel();
        LogIntent intent = null;
        switch (level.getName()) {
            case "FINE":
                intent = LogIntent.DEBUG;
                break;
            case "INFO":
                intent = LogIntent.INFO;
                break;
            case "WARNING":
                intent = LogIntent.WARNING;
                break;
            case "SEVERE":
                intent = LogIntent.SEVERE;
                break;
            default:
                intent = LogIntent.OTHER;
                break;
        }

        // Create and fire the event
        ServerLogTextEvent event = new ServerLogTextEvent(record.getMessage(), intent).fire();
        if (event.isCancelled()) {
            return;
        }
    }
}
