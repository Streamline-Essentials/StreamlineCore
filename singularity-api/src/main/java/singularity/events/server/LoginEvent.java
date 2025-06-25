package singularity.events.server;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

public class LoginEvent extends CosmicSenderEvent {
    public LoginEvent(CosmicPlayer player) {
        super(player);
    }
}
