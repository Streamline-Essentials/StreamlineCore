package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavableUser;

public class LoadSavableUserEvent<T extends SavableUser> extends SavableUserEvent<T> {
    public LoadSavableUserEvent(T user) {
        super(user);
    }
}
