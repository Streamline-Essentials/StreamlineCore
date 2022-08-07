package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlinePlayer;

public class LevelChangePlayerEvent extends ExperienceStreamlinePlayerEvent {
    @Getter @Setter
    private int oldLevel;

    public LevelChangePlayerEvent(StreamlinePlayer player, int oldLevel) {
        super(player);
        this.oldLevel = oldLevel;
    }
}
