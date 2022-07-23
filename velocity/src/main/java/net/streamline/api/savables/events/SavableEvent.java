package net.streamline.api.savables.events;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.SavableResource;

public abstract class SavableEvent<T extends SavableResource> extends StreamlineEvent {
    @Getter
    private final T resource;

    public SavableEvent(T resource) {
        super();
        this.resource = resource;
    }
}
