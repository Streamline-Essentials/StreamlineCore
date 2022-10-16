package net.streamline.api.savables.events;

import net.streamline.api.savables.SavableResource;

public class CreateSavableResourceEvent<T extends SavableResource> extends SavableEvent<T> {
    public CreateSavableResourceEvent(T resource) {
        super(resource);
    }
}
