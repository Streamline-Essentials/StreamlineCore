package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class DeleteSenderEvent extends StreamSenderEvent {
    public DeleteSenderEvent(CosmicSender player) {
        super(player);
    }
}
