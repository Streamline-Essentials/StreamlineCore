package net.streamline.api.savables.events;

import net.streamline.api.savables.users.StreamlineUser;

public class StreamlineUserEvent<T extends StreamlineUser> extends SavableEvent<T> {
    public StreamlineUserEvent(T user) {
        super(user);
    }
}
