package net.streamline.api.events.server;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.events.StreamSenderEvent;

@Getter
public class StreamlineChatEvent extends StreamSenderEvent {
    private final String originalMessage;
    @Setter
    private String message;
    @Setter
    private boolean canceled;

    public StreamlineChatEvent(StreamPlayer sender, String message) {
        super(sender);
        this.originalMessage = message;
        this.message = message;
        this.canceled = false;
    }
}
