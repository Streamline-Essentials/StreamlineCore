package net.streamline.api.data.players.events;

import net.streamline.api.data.players.StreamPlayer;

public class CreateSenderEvent extends StreamSenderEvent {
    public CreateSenderEvent(StreamPlayer player) {
        super(player);
    }
}
