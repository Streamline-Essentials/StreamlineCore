package singularity.events.server;

import lombok.Getter;
import singularity.events.CosmicEvent;
import singularity.logging.LogIntent;

@Getter
public class ServerLogTextEvent extends CosmicEvent {
    private final String message;
    private final LogIntent intent;

    public ServerLogTextEvent(String message, LogIntent intent) {
        this.message = message;
        this.intent = intent;
    }
}
