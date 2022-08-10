package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent {
    @Getter
    private final Date firedAt;
    @Getter @Setter
    private boolean completed;
    @Getter @Setter
    private boolean cancelled;

    public StreamlineEvent() {
        this.firedAt = new Date();
        this.completed = false;
        this.cancelled = false;
    }

    public String getEventName() {
        return getClass().getSimpleName();
    }
}
