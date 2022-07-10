package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavablePlayer;

public class SavablePlayerEvent extends SavableUserEvent {
    public SavablePlayer player() {
        return (SavablePlayer) this.user;
    }

    public SavablePlayerEvent(SavablePlayer player) {
        super(player);
    }
}
