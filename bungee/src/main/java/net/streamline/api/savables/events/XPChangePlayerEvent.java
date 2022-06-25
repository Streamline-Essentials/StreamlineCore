package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavablePlayer;

public class XPChangePlayerEvent extends ExperienceSavablePlayerEvent {
    public float oldXP;

    public XPChangePlayerEvent(SavablePlayer player, float oldXP) {
        super(player);
        this.oldXP = oldXP;
    }
}
