package net.streamline.api.events;


import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;
import net.streamline.utils.MessagingUtils;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent<T> extends AsyncEvent<T> {
    public Date firedAt;

    public StreamlineEvent() {
        super(new Callback<T>() {
            @Override
            public void done(T result, Throwable error) {
                MessagingUtils.logWarning("A module tried to run a callback on an event when it is disabled!");
            }
        });
        this.firedAt = new Date();
    }
}
