package net.streamline.platform.messaging;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.concurrent.ConcurrentSkipListSet;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getOnlinePlayers().size() == 0) return;

        if (! Streamline.getInstance().getServerNames().contains(message.getServer())) {
            Streamline.getInstance().getServerNames().forEach(a -> {
//                Streamline.getInstance().getProxy().getServerInfo(a).sendData(message.getChannel(), message.getMessages());

                if (UserManager.getInstance().getUsersOn(a).size() <= 0) {
                    MessageUtils.logInfo(a + " server is empty...");
                    return;
                }
                ProxiedPlayer player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(a).first().getUuid());
                if (player == null) {
                    return;
                }
                player.getServer().sendData(message.getMainChannel(), message.read());
            });
            return;
        }

//        Streamline.getInstance().getProxy().getServerInfo(message.getServer()).sendData(message.getChannel(), message.getMessages());

        ConcurrentSkipListSet<StreamlineUser> users = UserManager.getInstance().getUsersOn(message.getServer());
        if (users.isEmpty()) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        ProxiedPlayer player = Streamline.getPlayer(users.first().getUuid());
        if (player == null) {
            return;
        }
        player.getServer().sendData(message.getMainChannel(), message.read());
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
    }
}
