package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavableUser;

public class UnloadSavableUserEvent<T extends SavableUser> extends SavableUserEvent {
    public UnloadSavableUserEvent(T user) {
        super(user);
    }
}
