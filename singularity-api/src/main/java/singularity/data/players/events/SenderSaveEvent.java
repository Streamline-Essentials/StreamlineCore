package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class SenderSaveEvent extends StreamSenderEvent {
    public SenderSaveEvent(CosmicSender sender) {
        super(sender);
    }
}
