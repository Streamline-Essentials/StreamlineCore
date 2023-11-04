package net.streamline.api.savables.events;

import net.streamline.api.savables.users.StreamlineUser;

public class UnloadStreamlineUserEvent<T extends StreamlineUser> extends StreamlineUserEvent<T> {
    public UnloadStreamlineUserEvent(T user) {
        super(user);
    }
}
