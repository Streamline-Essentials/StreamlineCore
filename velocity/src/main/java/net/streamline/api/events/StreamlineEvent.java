package net.streamline.api.events;


import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent<T> extends CompletableFuture<T> {
    public Date firedAt;
    public Class<T> type;

    public StreamlineEvent(Class<T> type) {
        this.firedAt = new Date();
        this.type = type;
    }
}
