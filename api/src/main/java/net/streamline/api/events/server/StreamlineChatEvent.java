package net.streamline.api.events.server;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class StreamlineChatEvent extends StreamlineEvent {
    @Getter
    private final StreamlinePlayer sender;
    @Getter
    private final String originalMessage;
    @Getter @Setter
    private String message;
    @Getter @Setter
    private boolean canceled;

    public StreamlineChatEvent(StreamlinePlayer sender, String message) {
        this.sender = sender;
        this.originalMessage = message;
        this.message = message;
        this.canceled = false;
    }
}
