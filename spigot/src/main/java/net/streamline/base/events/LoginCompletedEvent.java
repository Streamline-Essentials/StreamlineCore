package net.streamline.base.events;

import net.streamline.api.events.server.LoginEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class LoginCompletedEvent extends LoginEvent {
    public LoginCompletedEvent(StreamlinePlayer resource) {
        super(resource);
    }
}
