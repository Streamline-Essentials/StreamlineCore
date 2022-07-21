package net.streamline.base.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.users.SavablePlayer;

public class StreamlineChatEvent extends StreamlineEvent<Boolean> {
    @Getter
    private final SavablePlayer sender;
    @Getter
    private final String originalMessage;
    @Getter @Setter
    private String message;
    @Getter @Setter
    private boolean canceled;

    public StreamlineChatEvent(SavablePlayer sender, String message) {
        this.sender = sender;
        this.originalMessage = message;
        this.message = message;
        this.canceled = false;
    }
}
