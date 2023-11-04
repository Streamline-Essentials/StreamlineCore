package net.streamline.api.savables.events;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.StreamlineResource;

public abstract class SavableEvent<T extends StreamlineResource> extends StreamlineEvent {
    @Getter
    private final T resource;

    public SavableEvent(T resource) {
        super();
        this.resource = resource;
    }
}
