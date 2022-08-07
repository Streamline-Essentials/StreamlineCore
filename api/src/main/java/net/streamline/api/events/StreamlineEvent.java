package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public abstract class StreamlineEvent extends CompletableFuture<Void> {
    @Getter
    private final Date firedAt;
    @Getter @Setter
    private boolean completed;

    public StreamlineEvent() {
        this.firedAt = new Date();
        this.completed = false;
    }

    public String getEventName() {
        return getClass().getSimpleName();
    }

    public void complete() {
        try {
            get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
