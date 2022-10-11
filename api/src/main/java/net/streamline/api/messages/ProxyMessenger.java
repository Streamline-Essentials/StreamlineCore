package net.streamline.api.messages;

import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;

public interface ProxyMessenger {
    void sendMessage(ProxiedMessage message);

    void receiveMessage(ProxyMessageInEvent event);
}
