package net.streamline.api.events.server;

import net.streamline.api.savables.events.SavableEvent;
import net.streamline.api.savables.users.SavablePlayer;

public class LoginEvent extends SavableEvent<SavablePlayer> {
    public LoginEvent(SavablePlayer resource) {
        super(resource);
    }
}
