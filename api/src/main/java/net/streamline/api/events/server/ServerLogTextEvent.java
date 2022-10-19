package net.streamline.api.events.server;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;

import java.util.logging.LogRecord;

public class ServerLogTextEvent extends StreamlineEvent {
    @Getter
    final LogRecord record;

    public ServerLogTextEvent(LogRecord record) {
        this.record = record;
    }

    public String getMessage() {
        return getRecord().getMessage();
    }
}
