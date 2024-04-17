package net.streamline.platform.messaging;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getOnlinePlayers().isEmpty()) return;

        StreamPlayer carrier = message.getCarrier();
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
