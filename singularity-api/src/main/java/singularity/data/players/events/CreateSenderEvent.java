package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class CreateSenderEvent extends CosmicSenderEvent {
    public CreateSenderEvent(CosmicSender player) {
        super(player);
    }
}
