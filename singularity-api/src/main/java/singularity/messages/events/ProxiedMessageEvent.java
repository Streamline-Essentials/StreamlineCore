package singularity.messages.events;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;
import singularity.messages.proxied.ProxiedMessage;

/**
 * Base event class for Singularity events that are backed by a {@link ProxiedMessage}.
 *
 * <p>Provides convenience delegate methods so that listeners can read the most
 * common message attributes (carrier, origin, sub-channel, payload fields) directly
 * from the event without having to call {@link #getMessage()} first. All delegate
 * methods return {@code null} / {@code false} when the wrapped message is
 * {@code null}.</p>
 *
 * <p>Concrete subclasses such as {@link ProxyMessageInEvent} specialise the event
 * for specific message-flow directions.</p>
 */
@Getter
public class ProxiedMessageEvent extends CosmicEvent {

    /**
     * The {@link ProxiedMessage} that triggered this event, or {@code null} if
     * no message is associated.
     */
    private final ProxiedMessage message;

    /**
     * Creates a new {@code ProxiedMessageEvent} wrapping the given message.
     *
     * @param message the {@link ProxiedMessage} associated with this event,
     *                may be {@code null}
     */
    public ProxiedMessageEvent(ProxiedMessage message) {
        this.message = message;
    }

    /**
     * Returns the {@link CosmicPlayer} that carried the underlying plugin message,
     * or {@code null} if the message is absent.
     *
     * @return the carrier player, or {@code null}
     */
    public CosmicPlayer getCarrier() {
        if (message == null) return null;
        return message.getCarrier();
    }

    /**
     * Returns {@code true} if the underlying message originated on the proxy side.
     *
     * @return {@code false} when the message is absent or backend-originated
     */
    public boolean isProxyOriginated() {
        if (message == null) return false;
        return message.isProxyOriginated();
    }

    /**
     * Returns the plugin-messaging sub-channel name of the underlying message,
     * or {@code null} if the message is absent.
     *
     * @return the sub-channel string, or {@code null}
     */
    public String getSubChannel() {
        if (message == null) return null;
        return message.getSubChannel();
    }

    /**
     * Returns {@code true} if the underlying message contains a value for the
     * specified payload key.
     *
     * @param key the payload field name to check
     * @return {@code false} when the message is absent or the key is not present
     */
    public boolean hasKey(String key) {
        if (message == null) return false;
        return message.hasKey(key);
    }

    /**
     * Returns the string value associated with the given payload key in the
     * underlying message, or {@code null} if the message is absent.
     *
     * @param key the payload field name to look up
     * @return the associated string value, or {@code null}
     */
    public String getString(String key) {
        if (message == null) return null;
        return message.getString(key);
    }
}
