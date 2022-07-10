package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavableUser;

public class LoadSavableUserEvent extends SavableUserEvent {
    public LoadSavableUserEvent(SavableUser user) {
        super(user);
    }
}
