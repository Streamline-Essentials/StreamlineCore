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

public class ProxyPluginMessenger implements ProxyMessenger {
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

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        // implemented else where.
    }
}
