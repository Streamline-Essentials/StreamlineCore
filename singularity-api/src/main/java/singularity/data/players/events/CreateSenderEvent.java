package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class CreateSenderEvent extends StreamSenderEvent {
    public CreateSenderEvent(CosmicSender player) {
        super(player);
    }
}
