package net.streamline.api.events.server;

import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.events.StreamSenderEvent;

public class LogoutEvent extends StreamSenderEvent {
    public LogoutEvent(StreamPlayer player) {
        super(player);
    }
}
