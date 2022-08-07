package net.streamline.api.events.server;

import net.streamline.api.savables.events.SavableEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class LogoutEvent extends SavableEvent<StreamlinePlayer> {
    public LogoutEvent(StreamlinePlayer resource) {
        super(resource);
    }
}
