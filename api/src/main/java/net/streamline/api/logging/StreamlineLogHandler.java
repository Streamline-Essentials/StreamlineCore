package net.streamline.api.logging;

import net.streamline.api.events.server.ServerLogTextEvent;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class StreamlineLogHandler extends StreamHandler {
    @Override
    public synchronized void publish(LogRecord record) {
        ServerLogTextEvent event = new ServerLogTextEvent(record).fire();
        if (event.isCancelled()) {
            return;
        }
        super.publish(event.getRecord());
    }
}
