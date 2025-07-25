package singularity.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import singularity.events.server.ServerLogTextEvent;

public class CosmicLogbackAppender extends AppenderBase<ILoggingEvent> {
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