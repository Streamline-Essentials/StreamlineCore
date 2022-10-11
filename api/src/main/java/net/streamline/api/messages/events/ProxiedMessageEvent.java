package net.streamline.api.messages.events;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.savables.users.StreamlinePlayer;

public class ProxiedMessageEvent extends StreamlineEvent {
    @Getter
    private final ProxiedMessage message;

    public ProxiedMessageEvent(ProxiedMessage message) {
        this.message = message;
    }
}
