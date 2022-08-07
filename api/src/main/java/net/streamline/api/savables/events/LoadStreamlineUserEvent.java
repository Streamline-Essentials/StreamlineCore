package net.streamline.api.savables.events;

import net.streamline.api.savables.users.StreamlineUser;

public class LoadStreamlineUserEvent<T extends StreamlineUser> extends StreamlineUserEvent<T> {
    public LoadStreamlineUserEvent(T user) {
        super(user);
    }
}
