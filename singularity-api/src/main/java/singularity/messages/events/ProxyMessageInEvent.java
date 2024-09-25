package singularity.messages.events;

import singularity.messages.proxied.ProxiedMessage;

public class ProxyMessageInEvent extends ProxiedMessageEvent {
    public ProxyMessageInEvent(ProxiedMessage message) {
        super(message);
    }
}
