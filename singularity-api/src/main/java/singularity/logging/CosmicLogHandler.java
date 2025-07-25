package singularity.logging;

import singularity.events.server.ServerLogTextEvent;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.logging.Level;

public class CosmicLogHandler extends StreamHandler {
    public CosmicLogHandler() {
        // Set output to System.out by default
        super.setOutputStream(System.out);
    }

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
