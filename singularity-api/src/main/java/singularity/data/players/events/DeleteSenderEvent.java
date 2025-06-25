package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class DeleteSenderEvent extends CosmicSenderEvent {
    public DeleteSenderEvent(CosmicSender player) {
        super(player);
    }
}
