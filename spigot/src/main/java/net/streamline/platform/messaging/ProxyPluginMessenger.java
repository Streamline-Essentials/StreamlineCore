package net.streamline.platform.messaging;

import net.streamline.api.SLAPI;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.messages.builders.ServerInfoMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;

import java.util.ArrayList;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxiedMessage message) {
        if (Streamline.getInstance().getOnlinePlayers().size() == 0) return;

        new ArrayList<>(Streamline.getInstance().getProxy().getOnlinePlayers()).get(0)
                .sendPluginMessage(Streamline.getInstance(), message.getMainChannel(), message.read());
    }

    @Override
    public void receiveMessage(ProxyMessageInEvent event) {
        if (event.getMessage().getMainChannel().equals(SLAPI.getApiChannel())) {
            if (event.getMessage().getSubChannel().equals(SavablePlayerMessageBuilder.getSubChannel())) {
                UserUtils.loadUser(new StreamlinePlayer(SavablePlayerMessageBuilder.unbuild(event.getMessage())));
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
