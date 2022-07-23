package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.SavablePlayer;

public class XPChangePlayerEvent extends ExperienceSavablePlayerEvent {
    @Getter @Setter
    private float oldXP;

    public XPChangePlayerEvent(SavablePlayer player, float oldXP) {
        super(player);
        this.oldXP = oldXP;
    }
}
