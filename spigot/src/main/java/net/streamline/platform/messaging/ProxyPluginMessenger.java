package net.streamline.platform.messaging;

import net.streamline.api.SLAPI;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getProxy().getOnlinePlayers().isEmpty()) {
            ProxiedMessageManager.pendMessage(message);
            return;
        }

        new ArrayList<>(Streamline.getInstance().getProxy().getOnlinePlayers()).get(0)
                .sendPluginMessage(Streamline.getInstance(), message.getMainChannel(), message.read());
    }

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        ProxiedMessageManager.onProxiedMessageReceived(event.getMessage());
        if (event.getMessage().getMainChannel().equals(SLAPI.getApiChannel())) {
            if (event.getMessage().getSubChannel().equals(SavablePlayerMessageBuilder.getSubChannel())) {
                ProxiedStreamlinePlayer proxiedStreamlinePlayer = SavablePlayerMessageBuilder.unbuild(event.getMessage());
                StreamlineUser user = UserUtils.getOrGetUser(proxiedStreamlinePlayer.getUuid());
                if (user != null) UserUtils.unloadUser(user);
                UserUtils.loadUser(new StreamlinePlayer(proxiedStreamlinePlayer));
            }
            if (event.getMessage().getSubChannel().equals(ResourcePackMessageBuilder.getSubChannel())) {
                SingleSet<String, StreamlineResourcePack> set = ResourcePackMessageBuilder.unbuild(event.getMessage());
                StreamlineResourcePack resourcePack = set.value;

                Streamline.getInstance().sendResourcePack(resourcePack, set.key);
            }
            if (event.getMessage().getSubChannel().equals(ServerInfoMessageBuilder.getSubChannel())) {
                ServerInfoMessageBuilder.handle(event.getMessage());
            }
        }
    }
}
