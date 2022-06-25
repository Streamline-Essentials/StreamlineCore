package net.streamline.api.savables.events;

import net.streamline.api.savables.events.SavableEvent;
import net.streamline.api.savables.users.SavableUser;

public class UnloadSavableUserEvent extends SavableUserEvent {
    public UnloadSavableUserEvent(SavableUser user) {
        super(user);
    }
}
