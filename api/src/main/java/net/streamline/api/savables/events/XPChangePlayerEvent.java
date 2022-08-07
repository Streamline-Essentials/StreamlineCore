package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlinePlayer;

public class XPChangePlayerEvent extends ExperienceStreamlinePlayerEvent {
    @Getter @Setter
    private double oldXP;

    public XPChangePlayerEvent(StreamlinePlayer player, double oldXP) {
        super(player);
        this.oldXP = oldXP;
    }
}
