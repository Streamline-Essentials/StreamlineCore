package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class LoadStreamSenderEvent extends StreamSenderEvent {
    public LoadStreamSenderEvent(CosmicSender player) {
        super(player);
    }
}
