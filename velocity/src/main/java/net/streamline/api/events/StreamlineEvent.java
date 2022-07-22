package net.streamline.api.events;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent<T> extends CompletableFuture<T> {
    public Date firedAt;

    public StreamlineEvent() {
        this.firedAt = new Date();
    }

    public String getEventName() {
        return getClass().getSimpleName();
    }

    abstract HandlerList getHandlerList();
}
