package net.streamline.api.messages.events;

import net.streamline.api.messages.proxied.ProxiedMessage;

public class ProxyMessageOutEvent extends ProxiedMessageEvent {
    public ProxyMessageOutEvent(ProxiedMessage message) {
        super(message);
    }
}
