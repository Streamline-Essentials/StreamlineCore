package net.streamline.api.data.players.events;

import net.streamline.api.data.players.StreamPlayer;

public class UnloadStreamSenderEvent extends StreamSenderEvent {
    public UnloadStreamSenderEvent(StreamPlayer user) {
        super(user);
    }
}
