package gg.drak.thebase.savables.events;

import gg.drak.thebase.savables.SavableResource;

public class DeleteSavableResourceEvent<T extends SavableResource> extends SavableEvent<T> {
    public DeleteSavableResourceEvent(T resource) {
        super(resource);
    }
}
