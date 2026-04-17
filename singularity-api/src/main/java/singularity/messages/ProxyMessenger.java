package singularity.messages;

import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;

/**
 * Platform-agnostic contract for sending and receiving plugin-messaging-channel
 * packets between the proxy and backend servers.
 *
 * <p>Each platform module (Velocity, BungeeCord, Spigot) provides its own
 * implementation that bridges this interface to the native plugin-channel API.
 */
public interface ProxyMessenger {

    /**
     * Transmits a {@link ProxiedMessage} through the appropriate
     * plugin-messaging channel on the current platform.
     *
     * @param message the message to send; must not be {@code null}
     */
    void sendMessage(ProxiedMessage message);

    /**
     * Processes an inbound plugin-channel packet that has been wrapped in a
     * {@link ProxyMessageInEvent} by the platform listener.
     *
     * @param event the inbound message event; must not be {@code null}
     */
    void receiveMessage(ProxyMessageInEvent event);
}
