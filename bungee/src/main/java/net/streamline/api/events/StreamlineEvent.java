package net.streamline.api.events;


import lombok.Getter;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent<T> extends CompletableFuture<T> {
    @Getter
    private final static HandlerList handlerList = new HandlerList();

    public Date firedAt;

    public StreamlineEvent() {
        this.firedAt = new Date();
    }

    public String getEventName() {
        return getClass().getSimpleName();
    }
}
