package net.streamline.platform.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.streamline.api.messages.ProxyMessageEvent;
import net.streamline.api.messages.ProxyMessageOut;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.ServerConnectMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.Optional;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxyMessageOut message) {
        if (Streamline.getInstance().getOnlinePlayers().size() <= 0) return;

        if (message.getServer().equals("")) {
            Streamline.getInstance().getServerNames().forEach(a -> {
                Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(a);
                if (server.isEmpty()) return;
                server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());
            });
            return;
        }

        Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(message.getServer());
        if (server.isEmpty()) return;
        server.get().sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());

//        Player player = Streamline.getPlayer(ModuleUtils.getUsersOn(message.getServer()).get(0).getUUID());
//        if (player == null) return;
//        player.sendPluginMessage(MinecraftChannelIdentifier.from(message.getChannel()), message.getMessages());
    }

    @Override
    public void receiveMessage(ProxyMessageEvent event) {
        if (event.getMessage().getSubChannel().equals(ServerConnectMessageBuilder.getSubChannel())) {
            SingleSet<String, String> set = ServerConnectMessageBuilder.unbuild(event.getMessage());
            Optional<RegisteredServer> server = Streamline.getInstance().getProxy().getServer(set.key);
            if (server.isEmpty()) return;
            StreamlineUser player = UserManager.getInstance().getOrGetUser(set.value);
            UserManager.getInstance().connect(player, set.key);
        }
    }
}
