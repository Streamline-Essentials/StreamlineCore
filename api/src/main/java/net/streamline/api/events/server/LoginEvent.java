package net.streamline.api.events.server;

import net.streamline.api.savables.events.SavableEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class LoginEvent extends SavableEvent<StreamlinePlayer> {
    public LoginEvent(StreamlinePlayer resource) {
        super(resource);
    }
}
