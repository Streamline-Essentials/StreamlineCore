package singularity.events.server;

import lombok.Getter;
import singularity.events.CosmicEvent;

import java.util.logging.LogRecord;

@Getter
public class ServerLogTextEvent extends CosmicEvent {
    final LogRecord record;

    public ServerLogTextEvent(LogRecord record) {
        this.record = record;
    }

    public String getMessage() {
        return getRecord().getMessage();
    }
}
