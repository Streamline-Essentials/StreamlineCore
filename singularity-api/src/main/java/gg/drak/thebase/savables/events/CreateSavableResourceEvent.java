package gg.drak.thebase.savables.events;

import gg.drak.thebase.savables.SavableResource;

public class CreateSavableResourceEvent<T extends SavableResource> extends SavableEvent<T> {
    public CreateSavableResourceEvent(T resource) {
        super(resource);
    }
}
