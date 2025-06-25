package singularity.data.players.events;

import singularity.data.console.CosmicSender;

public class SaveSenderEvent extends CosmicSenderEvent {
    public SaveSenderEvent(CosmicSender sender) {
        super(sender);
    }
}
