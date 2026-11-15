package gg.drak.thebase.savables.events;

import lombok.Getter;
import gg.drak.thebase.events.components.BaseEvent;
import gg.drak.thebase.savables.SavableResource;

public abstract class SavableEvent<T extends SavableResource> extends BaseEvent {
    @Getter
    private final T resource;

    public SavableEvent(T resource) {
        super();
        this.resource = resource;
    }
}
