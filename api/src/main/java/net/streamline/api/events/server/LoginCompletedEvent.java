package net.streamline.api.events.server;

import net.streamline.api.data.players.StreamPlayer;

public class LoginCompletedEvent extends LoginEvent {
    public LoginCompletedEvent(StreamPlayer player) {
        super(player);
    }
}
