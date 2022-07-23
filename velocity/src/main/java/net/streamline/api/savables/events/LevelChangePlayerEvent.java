package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.SavablePlayer;

public class LevelChangePlayerEvent extends ExperienceSavablePlayerEvent {
    @Getter @Setter
    private int oldLevel;

    public LevelChangePlayerEvent(SavablePlayer player, int oldLevel) {
        super(player);
        this.oldLevel = oldLevel;
    }
}
