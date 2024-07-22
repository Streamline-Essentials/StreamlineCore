package net.streamline.api.data.players.events;

import net.streamline.api.data.console.StreamSender;

public class CreateSenderEvent extends StreamSenderEvent {
    public CreateSenderEvent(StreamSender player) {
        super(player);
    }
}
