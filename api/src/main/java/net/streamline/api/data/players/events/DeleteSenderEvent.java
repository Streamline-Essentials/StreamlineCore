package net.streamline.api.data.players.events;

import net.streamline.api.data.console.StreamSender;

public class DeleteSenderEvent extends StreamSenderEvent {
    public DeleteSenderEvent(StreamSender player) {
        super(player);
    }
}
