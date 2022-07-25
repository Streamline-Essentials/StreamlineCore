package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.SavablePlayer;

public class XPChangePlayerEvent extends ExperienceSavablePlayerEvent {
    @Getter @Setter
    private double oldXP;

    public XPChangePlayerEvent(SavablePlayer player, double oldXP) {
        super(player);
        this.oldXP = oldXP;
    }
}
