package net.streamline.api.messages;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.users.StreamlinePlayer;

public class ProxyMessageEvent extends StreamlineEvent {
    @Getter
    private final ProxyMessageIn message;
    @Getter
    private final StreamlinePlayer carrier;

    public ProxyMessageEvent(ProxyMessageIn message, StreamlinePlayer carrier) {
        this.message = message;
        this.carrier = carrier;
    }
}
