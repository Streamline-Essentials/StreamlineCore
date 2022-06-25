package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavableUser;

public class SavableUserEvent extends SavableEvent {
    public SavableUser user;

    public SavableUserEvent(SavableUser user) {
        this.user = user;
    }
}
