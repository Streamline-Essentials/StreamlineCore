package net.streamline.platform.messaging;

import net.streamline.api.SLAPI;
import net.streamline.api.messages.ProxiedStreamPlayer;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.builders.*;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamPlayer;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;

import java.util.ArrayList;

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
            if (event.getMessage().getSubChannel().equals(ResourcePackMessageBuilder.getSubChannel())) {
                SingleSet<String, StreamlineResourcePack> set = ResourcePackMessageBuilder.unbuild(event.getMessage());
                StreamlineResourcePack resourcePack = set.getValue();

                Streamline.getInstance().sendResourcePack(resourcePack, set.getKey());
            }
            if (event.getMessage().getSubChannel().equals(ServerInfoMessageBuilder.getSubChannel())) {
                ServerInfoMessageBuilder.handle(event.getMessage());
            }
            if (event.getMessage().getSubChannel().equals(SavablePlayerMessageBuilder.getSubChannel())) {
                ProxiedStreamPlayer proxiedPlayer = SavablePlayerMessageBuilder.unbuild(event.getMessage());

                UserUtils.unloadUser(proxiedPlayer.getUuid());
                StreamPlayer player = new StreamPlayer(proxiedPlayer);
                UserUtils.loadUser(player);
            }
            if (event.getMessage().getSubChannel().equals(UserNameMessageBuilder.getSubChannel())) {
                UserNameMessageBuilder.handle(event.getMessage());
            }
            if (event.getMessage().getSubChannel().equals(TeleportMessageBuilder.getSubChannel())) {
                TeleportMessageBuilder.handle(event.getMessage());
            }
        }
    }
}
