package net.streamline.api.data.players.events;

import net.streamline.api.data.console.StreamSender;

public class LoadStreamSenderEvent extends StreamSenderEvent {
    public LoadStreamSenderEvent(StreamSender player) {
        super(player);
    }
}
