package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavablePlayer;

public class SavablePlayerEvent extends SavableUserEvent<SavablePlayer> {

    public SavablePlayerEvent(SavablePlayer player) {
        super(player);
    }
}
