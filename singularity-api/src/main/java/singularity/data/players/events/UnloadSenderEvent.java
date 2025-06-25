package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class UnloadSenderEvent extends CosmicSenderEvent {
    public UnloadSenderEvent(CosmicSender user) {
        super(user);
    }
}
