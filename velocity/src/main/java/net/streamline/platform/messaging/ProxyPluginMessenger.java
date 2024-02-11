package net.streamline.platform.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.messages.builders.UserNameMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.savables.UserManager;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (StreamlineVelocity.getInstance().getOnlinePlayers().isEmpty()) return;

        if (! StreamlineVelocity.getInstance().getServerNames().contains(message.getServer())) {
            StreamlineVelocity.getInstance().getServerNames().forEach(a -> {
                Optional<RegisteredServer> server = StreamlineVelocity.getInstance().getProxy().getServer(a);
                if (server.isEmpty()) {
                    return;
                }
                server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read());

                if (UserManager.getInstance().getUsersOn(a).size() <= 0) {
                    return;
                }
                Player player = StreamlineVelocity.getPlayer(UserManager.getInstance().getUsersOn(a).first().getUuid());
                if (player == null) {
                    return;
                }
                if (player.getCurrentServer().isEmpty()) {
                    ProxiedMessageManager.pendMessage(message);
                    return;
                }
                player.getCurrentServer().get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read());
            });
            return;
        }

        ConcurrentSkipListSet<StreamlineUser> users = UserManager.getInstance().getUsersOn(message.getServer());
        if (users.isEmpty()) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }
        Player player = StreamlineVelocity.getPlayer(users.first().getUuid());
        if (player == null) {
            return;
        }
        if (player.getCurrentServer().isEmpty()) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        player.getCurrentServer().get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read());
    }

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        ProxiedMessageManager.onProxiedMessageReceived(event.getMessage());
        if (event.getMessage().getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
            ServerConnectMessageBuilder.handle(event.getMessage());
            return;
        }
        if (event.getMessage().getSubChannel().equals(ProxyParseMessageBuilder.getSubChannel())) {
            ProxyParseMessageBuilder.handle(event.getMessage());
            return;
        }
        if (event.getMessage().getSubChannel().equals(UserNameMessageBuilder.getSubChannel())) {
            UserNameMessageBuilder.handle(event.getMessage());
        }
    }
}
