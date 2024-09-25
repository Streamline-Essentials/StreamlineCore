package singularity.events.server;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.StreamSenderEvent;

public class LoginEvent extends StreamSenderEvent {
    public LoginEvent(CosmicPlayer player) {
        super(player);
    }
}
