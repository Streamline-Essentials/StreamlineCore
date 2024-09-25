package singularity.events.server;

import singularity.data.players.CosmicPlayer;

public class LoginCompletedEvent extends LoginEvent {
    public LoginCompletedEvent(CosmicPlayer player) {
        super(player);
    }
}
