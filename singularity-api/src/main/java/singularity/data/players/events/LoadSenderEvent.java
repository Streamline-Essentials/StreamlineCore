package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class LoadSenderEvent extends CosmicSenderEvent {
    public LoadSenderEvent(CosmicSender player) {
        super(player);
    }
}
