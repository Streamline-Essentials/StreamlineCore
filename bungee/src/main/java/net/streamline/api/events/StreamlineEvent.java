package net.streamline.api.events;


import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;
import net.streamline.utils.MessagingUtils;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent<T> extends CompletableFuture<T> {
    public Date firedAt;

    public StreamlineEvent() {
        this.firedAt = new Date();
    }
}
