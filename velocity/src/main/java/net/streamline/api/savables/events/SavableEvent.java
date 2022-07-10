package net.streamline.api.savables.events;

import net.streamline.api.events.StreamlineEvent;

public abstract class SavableEvent extends StreamlineEvent<Boolean> {
    public SavableEvent() {
        super(Boolean.class);
    }
}
