package net.streamline.platform.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ReturnParseMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import net.streamline.platform.savables.UserManager;

import java.util.Optional;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxyMessageOut message) {
        if (Streamline.getInstance().getOnlinePlayers().size() <= 0) return;

        if (! Streamline.getInstance().getServerNames().contains(message.getServer())) {
            Streamline.getInstance().getServerNames().forEach(a -> {
                Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(a);
                if (server.isEmpty()) {
                    return;
                }
                server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());

                if (UserManager.getInstance().getUsersOn(a).size() <= 0) {
//                    Messenger.getInstance().logInfo(a + " server is empty...");
                    return;
                }
                Player player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(a).first().getUuid());
                if (player == null) {
//                    Messenger.getInstance().logInfo("Player = null...");
                    return;
                }
                player.getCurrentServer().get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());
            });
            return;
        }

//        Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(message.getServer());
//        if (server.isEmpty()) {
//            return;
//        }
//        server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());

        if (UserManager.getInstance().getUsersOn(message.getServer()).size() <= 0) {
//                    Messenger.getInstance().logInfo(a + " server is empty...");
            MessageQueue.queue(message);
            return;
        }
        Player player = Streamline.getPlayer(UserManager.getInstance().getUsersOn(message.getServer()).first().getUuid());
        if (player == null) {
//                    Messenger.getInstance().logInfo("Player = null...");
            return;
        }
        player.getCurrentServer().get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());
    }

    @Override
    public void receiveMessage(ProxyMessageEvent event) {
        if (event.getMessage().getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
            SingleSet<String, String> set = ServerConnectMessageBuilder.unbuild(event.getMessage());
            Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(set.key);
            if (server.isEmpty()) return;
            StreamlineUser player = UserUtils.getOrGetUser(set.value);
            if (player == null) return;
            UserManager.getInstance().connect(player, set.key);
        }
        if (event.getMessage().getSubChannel().equals(ProxyParseMessageBuilder.getSubChannel())) {
            SingleSet<String, String> set = ProxyParseMessageBuilder.unbuild(event.getMessage());
            StreamlineUser user = UserUtils.getOrGetUser(set.value);
            if (user == null) return;
            SLAPI.getInstance().getProxyMessenger().sendMessage(ReturnParseMessageBuilder.build(set.key, Messenger.getInstance().replaceAllPlayerBungee(user, set.key), user));
        }
    }
}
