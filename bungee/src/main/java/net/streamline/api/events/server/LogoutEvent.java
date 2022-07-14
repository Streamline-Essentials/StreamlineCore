package net.streamline.api.events.server;

import net.streamline.api.savables.events.SavableEvent;
import net.streamline.api.savables.users.SavablePlayer;

public class LogoutEvent extends SavableEvent<SavablePlayer> {
    public LogoutEvent(SavablePlayer resource) {
        super(resource);
    }
}
