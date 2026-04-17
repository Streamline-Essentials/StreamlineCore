package singularity.messages.events;

import singularity.messages.proxied.ProxiedMessage;

/**
 * Event fired when a proxied message is about to be sent outbound through the
 * plugin messaging channel.  Listeners can inspect the wrapped
 * {@link ProxiedMessage} before it leaves the current side of the network.
 */
public class ProxyMessageOutEvent extends ProxiedMessageEvent {

    /**
     * Constructs an outbound proxy-message event wrapping the given message.
     *
     * @param message the {@link ProxiedMessage} that is being dispatched
     */
    public ProxyMessageOutEvent(ProxiedMessage message) {
        super(message);
    }
}
