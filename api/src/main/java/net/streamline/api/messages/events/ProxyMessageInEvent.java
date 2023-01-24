package net.streamline.api.messages.events;

import net.streamline.api.messages.proxied.ProxiedMessage;

public class ProxyMessageInEvent extends ProxiedMessageEvent {
    public ProxyMessageInEvent(ProxiedMessage message) {
        super(message);
    }
}
