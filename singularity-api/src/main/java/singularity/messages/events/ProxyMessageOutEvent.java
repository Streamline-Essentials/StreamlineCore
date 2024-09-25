package singularity.messages.events;

import singularity.messages.proxied.ProxiedMessage;

public class ProxyMessageOutEvent extends ProxiedMessageEvent {
    public ProxyMessageOutEvent(ProxiedMessage message) {
        super(message);
    }
}
