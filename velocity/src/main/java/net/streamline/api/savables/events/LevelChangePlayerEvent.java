package net.streamline.api.savables.events;

import net.streamline.api.savables.users.SavablePlayer;

public class LevelChangePlayerEvent extends ExperienceSavablePlayerEvent {
    public int oldLevel;

    public LevelChangePlayerEvent(SavablePlayer player, int oldLevel) {
        super(player);
        this.oldLevel = oldLevel;
    }
}
