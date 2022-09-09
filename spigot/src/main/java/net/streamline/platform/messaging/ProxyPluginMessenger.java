package net.streamline.platform.messaging;

import net.streamline.api.SLAPI;
import net.streamline.api.messages.*;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.users.SavablePlayer;

import java.util.ArrayList;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxyMessageOut message) {
        if (Streamline.getInstance().getOnlinePlayers().size() <= 0) return;

        new ArrayList<>(Streamline.getInstance().getProxy().getOnlinePlayers()).get(0)
                .sendPluginMessage(Streamline.getInstance(), message.getChannel(), message.getMessages());
    }

    @Override
    public void receiveMessage(ProxyMessageEvent event) {
        ProxyMessageIn message = event.getMessage();

        if (message.getChannel().equals(SLAPI.getApiChannel())) {
            if (message.getSubChannel().equals(SavablePlayerMessageBuilder.getSubChannel())) {
                UserManager.getInstance().loadUser(new SavablePlayer(SavablePlayerMessageBuilder.unbuild(message)));
            }
            if (message.getSubChannel().equals(ResourcePackMessageBuilder.getSubChannel())) {
                SingleSet<String, StreamlineResourcePack> set = ResourcePackMessageBuilder.unbuild(message);
                StreamlineResourcePack resourcePack = set.value;

                Streamline.getInstance().sendResourcePack(resourcePack, set.key);
            }
        }
    }
}
