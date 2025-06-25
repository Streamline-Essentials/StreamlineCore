package singularity.events.server;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

public class LogoutEvent extends CosmicSenderEvent {
    public LogoutEvent(CosmicPlayer player) {
        super(player);
    }
}
