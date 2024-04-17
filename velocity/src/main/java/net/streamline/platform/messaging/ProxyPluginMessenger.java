package net.streamline.platform.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.savables.UserManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (StreamlineVelocity.getInstance().getOnlinePlayers().isEmpty()) return;

        StreamPlayer carrier = message.getCarrier();
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
