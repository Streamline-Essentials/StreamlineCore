package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

public class UnloadStreamSenderEvent extends StreamSenderEvent {
    public UnloadStreamSenderEvent(CosmicPlayer user) {
        super(user);
    }
}
