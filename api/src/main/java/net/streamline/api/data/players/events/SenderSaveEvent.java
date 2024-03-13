package net.streamline.api.data.players.events;

import net.streamline.api.data.console.StreamSender;

public class SenderSaveEvent extends StreamSenderEvent {
    public SenderSaveEvent(StreamSender sender) {
        super(sender);
    }
}
