package singularity.events.server;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.StreamSenderEvent;

@Getter
public class CosmicChatEvent extends StreamSenderEvent {
    private final String originalMessage;
    @Setter
    private String message;
    @Setter
    private boolean canceled;

    public CosmicChatEvent(CosmicPlayer sender, String message) {
        super(sender);
        this.originalMessage = message;
        this.message = message;
        this.canceled = false;
    }
}
