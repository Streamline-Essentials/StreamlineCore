package net.streamline.api.data.players.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;

@Getter @Setter
public class LevelChangeEvent extends ExperienceEvent {
    private int newLevel;
    private int oldLevel;

    public LevelChangeEvent(StreamSender player, int newLevel, int oldLevel) {
        super(player);
        this.newLevel = newLevel;
        this.oldLevel = oldLevel;
    }
}
