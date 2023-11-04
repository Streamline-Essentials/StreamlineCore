package net.streamline.api.events.server;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

@Getter
public class StreamlineChatEvent extends StreamlineEvent {
    private final StreamlinePlayer sender;
    private final String originalMessage;
    @Setter
    private String message;
    @Setter
    private boolean canceled;

    public StreamlineChatEvent(StreamlinePlayer sender, String message) {
        this.sender = sender;
        this.originalMessage = message;
        this.message = message;
        this.canceled = false;
    }
}
