package net.streamline.api.events.server;

import net.streamline.api.events.Event;

/**
 * Miscellaneous server events
 */
public abstract class ServerEvent extends Event {

    public ServerEvent() {
        super();
    }

    public ServerEvent(boolean isAsync) {
        super(isAsync);
    }
}
