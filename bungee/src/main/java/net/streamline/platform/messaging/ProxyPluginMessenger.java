package net.streamline.platform.messaging;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.base.StreamlineBungee;
import singularity.data.players.CosmicPlayer;
import singularity.messages.ProxyMessenger;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.messages.proxied.ProxiedMessageManager;

import java.util.UUID;

/**
 * BungeeCord implementation of {@link ProxyMessenger} that routes cross-server
 * plugin messages through the BungeeCord plugin-messaging channel.
 *
 * <p>When a carrier player is online, the message is dispatched via
 * {@link net.md_5.bungee.api.connection.Server#sendData}. If the carrier
 * player is not currently online the message is queued via
 * {@link singularity.messages.proxied.ProxiedMessageManager#pendMessage}.
 */
public class ProxyPluginMessenger implements ProxyMessenger {

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the carrier player from the proxy player list. If no
     * players are online or the carrier is unavailable, the message is
     * pending-queued instead of sent.
     */
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (StreamlineBungee.getInstance().getOnlinePlayers().isEmpty()) return;

        CosmicPlayer carrier = message.getCarrier();
        if (carrier == null) {
            return;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(UUID.fromString(carrier.getUuid()));
        if (player == null) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        player.getServer().sendData(message.getMainChannel(), message.read());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Incoming messages are handled by
     * {@link net.streamline.platform.listeners.PlatformListener#onPluginMessage};
     * this method is intentionally left empty.
     */
    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        // implemented else where.
    }
}
