package singularity.logging;

import singularity.events.server.ServerLogTextEvent;

import java.io.ByteArrayOutputStream;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.logging.Level;

public class CosmicLogHandler extends StreamHandler {
    private final ByteArrayOutputStream baos;

    public CosmicLogHandler() {
        // Set output to System.out by default
        super.setOutputStream(System.out);

        this.baos = new ByteArrayOutputStream();
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

        // Capture the log message
        String formattedMessage = getFormatter().format(record);
        baos.write(formattedMessage.getBytes(), 0, formattedMessage.length());

        // Pass to parent handler for actual logging
        super.publish(record);
        super.flush(); // Ensure immediate output
    }

    // Method to get captured logs
    public String getCapturedLogs() {
        return baos.toString();
    }
}
