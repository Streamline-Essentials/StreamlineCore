package singularity.events.server;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.StreamSenderEvent;

public class LogoutEvent extends StreamSenderEvent {
    public LogoutEvent(CosmicPlayer player) {
        super(player);
    }
}
