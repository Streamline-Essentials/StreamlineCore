package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlinePlayer;

@Getter
public class XPChangePlayerEvent extends ExperienceStreamlinePlayerEvent {
    @Setter
    private double oldXP;

    public XPChangePlayerEvent(StreamlinePlayer player, double oldXP) {
        super(player);
        this.oldXP = oldXP;
    }
}
