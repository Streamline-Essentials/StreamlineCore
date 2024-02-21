package net.streamline.api.data.players.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;

@Getter @Setter
public class UserNameUpdateEvent extends StreamSenderEvent {
    private String changeTo;
    private final String changeFrom;

    public UserNameUpdateEvent(StreamPlayer player, String changeTo, String changeFrom) {
        super(player);
        this.changeTo = changeTo;
        this.changeFrom = changeFrom;
    }
}
