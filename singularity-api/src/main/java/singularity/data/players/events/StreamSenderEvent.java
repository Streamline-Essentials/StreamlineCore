package singularity.data.players.events;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;

@Getter @Setter
public class StreamSenderEvent extends CosmicEvent {
    private CosmicSender sender;

    public StreamSenderEvent(CosmicSender sender) {
        this.sender = sender;
    }

    public boolean isPlayer() {
        return sender instanceof CosmicPlayer;
    }

    public CosmicPlayer getPlayer() {
        if (! isPlayer()) return null;

        return (CosmicPlayer) sender;
    }
}
