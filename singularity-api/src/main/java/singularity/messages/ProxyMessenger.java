package singularity.messages;

import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;

public interface ProxyMessenger {
    void sendMessage(ProxiedMessage message);

    void receiveMessage(ProxyMessageInEvent event);
}
