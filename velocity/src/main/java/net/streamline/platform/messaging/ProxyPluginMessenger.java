package net.streamline.platform.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.Optional;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getOnlinePlayers().size() == 0) return;

        if (! Streamline.getInstance().getServerNames().contains(message.getServer())) {
            Streamline.getInstance().getServerNames().forEach(a -> {
                Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(a);
                if (server.isEmpty()) {
                    return;
                }
                server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read());

                if (UserManager.getInstance().getUsersOn(a).size() <= 0) {
//                    Messenger.getInstance().logInfo(a + " server is empty...");
                    return;
                }
                Player player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(a).first().getUuid());
                if (player == null) {
//                    Messenger.getInstance().logInfo("Player = null...");
                    return;
                }
                player.getCurrentServer().get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read());
            });
            return;
        }

//        Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(message.getServer());
//        if (server.isEmpty()) {
//            return;
//        }
//        server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());

        Player player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(message.getServer()).first().getUuid());
        if (player == null) {
//                    Messenger.getInstance().logInfo("Player = null...");
            return;
        }
        player.getCurrentServer().get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getMainChannel()), message.read());
    }

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        if (event.getMessage().getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
            ServerConnectMessageBuilder.handle(event.getMessage());
        }
        if (event.getMessage().getSubChannel().equals(ProxyParseMessageBuilder.getSubChannel())) {
            ProxyParseMessageBuilder.handle(event.getMessage());
        }
    }
}
