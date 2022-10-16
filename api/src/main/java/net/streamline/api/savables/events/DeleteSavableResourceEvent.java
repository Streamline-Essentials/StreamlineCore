package net.streamline.api.savables.events;

import net.streamline.api.savables.SavableResource;

public class DeleteSavableResourceEvent<T extends SavableResource> extends SavableEvent<T> {
    public DeleteSavableResourceEvent(T resource) {
        super(resource);
    }
}
