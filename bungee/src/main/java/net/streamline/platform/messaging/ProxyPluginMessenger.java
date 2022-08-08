package net.streamline.platform.messaging;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.ProxyMessageEvent;
import net.streamline.api.messages.ProxyMessageOut;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.ServerConnectMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.users.SavablePlayer;

import java.util.ArrayList;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxyMessageOut message) {
        if (Streamline.getInstance().getOnlinePlayers().size() <= 0) return;

        if (message.getServer().equals("")) {
            Streamline.getInstance().getServerNames().forEach(a -> {
                Streamline.getInstance().getProxy().getServerInfo(a).sendData(message.getChannel(), message.getMessages());
            });
            return;
        }

        ProxiedPlayer player = Streamline.getPlayer(ModuleUtils.getUsersOn(message.getServer()).get(0).getUUID());
        if (player == null) return;
        player.sendData(message.getChannel(), message.getMessages());
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
    }
}
