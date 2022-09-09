package net.streamline.platform.messaging;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ReturnParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import net.streamline.platform.savables.UserManager;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxyMessageOut message) {
        if (Streamline.getInstance().getOnlinePlayers().size() <= 0) return;

        if (! Streamline.getInstance().getServerNames().contains(message.getServer())) {
            Streamline.getInstance().getServerNames().forEach(a -> {
//                Streamline.getInstance().getProxy().getServerInfo(a).sendData(message.getChannel(), message.getMessages());

                if (UserManager.getInstance().getUsersOn(a).size() <= 0) {
                    Messenger.getInstance().logInfo(a + " server is empty...");
                    return;
                }
                ProxiedPlayer player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(a).get(0).getUUID());
                if (player == null) {
                    return;
                }
                player.getServer().sendData(message.getChannel(), message.getMessages());
            });
            return;
        }

//        Streamline.getInstance().getProxy().getServerInfo(message.getServer()).sendData(message.getChannel(), message.getMessages());

        if (UserManager.getInstance().getUsersOn(message.getServer()).size() <= 0) {
            MessageQueue.queue(message);
            return;
        }
        ProxiedPlayer player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(message.getServer()).get(0).getUUID());
        if (player == null) {
            return;
        }
        player.getServer().sendData(message.getChannel(), message.getMessages());
    }

    @Override
    public void receiveMessage(ProxyMessageEvent event) {
        if (event.getMessage().getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
            SingleSet<String, String> set = ServerConnectMessageBuilder.unbuild(event.getMessage());
            ServerInfo info = Streamline.getInstance().getProxy().getServerInfo(set.key);
            if (info == null) return;
            StreamlineUser player = UserManager.getInstance().getOrGetUser(set.value);
            UserManager.getInstance().connect(player, set.key);
        }
        if (event.getMessage().getSubChannel().equals(ProxyParseMessageBuilder.getSubChannel())) {
            SingleSet<String, String> set = ProxyParseMessageBuilder.unbuild(event.getMessage());
            StreamlineUser user = SLAPI.getInstance().getUserManager().getOrGetUser(set.value);
            SLAPI.getInstance().getProxyMessenger().sendMessage(ReturnParseMessageBuilder.build(set.key, Messenger.getInstance().replaceAllPlayerBungee(user, set.key), user));
        }
    }
}
