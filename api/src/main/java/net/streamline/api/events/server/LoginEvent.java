package net.streamline.api.events.server;

import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.events.StreamSenderEvent;

public class LoginEvent extends StreamSenderEvent {
    public LoginEvent(StreamPlayer player) {
        super(player);
    }
}
