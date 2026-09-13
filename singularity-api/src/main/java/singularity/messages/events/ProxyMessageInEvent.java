package singularity.messages.events;

import singularity.messages.proxied.ProxiedMessage;

/**
 * Fired by the Singularity event bus when an inbound {@link ProxiedMessage} is
 * received from the plugin-messaging channel.
 *
 * <p>Listeners can use the delegate accessors inherited from
 * {@link ProxiedMessageEvent} to inspect the message without unwrapping it
 * manually. Cancelling this event (if the underlying {@link singularity.events.CosmicEvent}
 * supports cancellation) prevents downstream handlers from processing the message.</p>
 */
public class ProxyMessageInEvent extends ProxiedMessageEvent {

    /**
     * Creates a new {@code ProxyMessageInEvent} wrapping the received message.
     *
     * @param message the inbound {@link ProxiedMessage} that was received
     */
    public ProxyMessageInEvent(ProxiedMessage message) {
        super(message);
    }
}
