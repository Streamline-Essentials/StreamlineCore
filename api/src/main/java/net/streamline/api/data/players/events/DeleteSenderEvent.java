package net.streamline.api.data.players.events;

import net.streamline.api.data.players.StreamPlayer;

public class DeleteSenderEvent extends StreamSenderEvent {
    public DeleteSenderEvent(StreamPlayer player) {
        super(player);
    }
}
