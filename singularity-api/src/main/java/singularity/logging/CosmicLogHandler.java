package singularity.logging;

import singularity.events.server.ServerLogTextEvent;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class CosmicLogHandler extends StreamHandler {
    @Override
    public synchronized void publish(LogRecord record) {
        ServerLogTextEvent event = new ServerLogTextEvent(record).fire();
        if (event.isCancelled()) {
            return;
        }
        super.publish(event.getRecord());
    }
}
