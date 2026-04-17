package net.streamline.platform.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.streamline.base.StreamlineVelocity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.messages.ProxyMessenger;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.messages.proxied.ProxiedMessageManager;

import java.util.UUID;

/**
 * Velocity implementation of {@link ProxyMessenger} that routes cross-server plugin messages
 * through the Velocity proxy channel API.
 *
 * <p>Outbound messages are sent via the carrier player's currently connected backend server
 * using a {@link MinecraftChannelIdentifier}. If the carrier player is not online the
 * message is deferred via {@link ProxiedMessageManager#pendMessage(ProxiedMessage)}.
 * Inbound message handling is implemented elsewhere via {@code PlatformListener}.
 */
public class ProxyPluginMessenger implements ProxyMessenger {
    /**
     * {@inheritDoc}
     *
     * <p>Resolves the carrier {@link Player} and forwards the message payload to the
     * backend server via plugin-messaging. If no players are online or the carrier player
     * is offline, the message is either dropped or pended for later delivery.
     *
     * @param message the {@link ProxiedMessage} to send; must have a non-null carrier
     */
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (StreamlineVelocity.getInstance().getOnlinePlayers().isEmpty()) return;

        CosmicPlayer carrier = message.getCarrier();
        if (carrier == null) {
            return;
        }

        Player player = StreamlineVelocity.getPlayer(UUID.fromString(carrier.getUuid()));
        if (player == null) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        player.getCurrentServer().ifPresent(server -> server.sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Inbound message processing is handled by {@code PlatformListener#onPluginMessage};
     * this method is intentionally a no-op.
     *
     * @param event the inbound proxy message event (unused here)
     */
    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        // implemented else where.
    }
}
