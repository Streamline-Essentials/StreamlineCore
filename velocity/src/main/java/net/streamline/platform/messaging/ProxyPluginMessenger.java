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

public class ProxyPluginMessenger implements ProxyMessenger {
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

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        // implemented else where.
    }
}
