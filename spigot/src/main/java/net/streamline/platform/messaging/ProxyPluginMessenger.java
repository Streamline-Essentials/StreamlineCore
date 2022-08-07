package net.streamline.platform.messaging;

import net.streamline.api.messages.ProxyMessageEvent;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.ProxyMessageOut;
import net.streamline.base.Streamline;

import java.util.ArrayList;

public class ProxyPluginMessenger implements ProxyMessenger {
    @Override
    public void sendMessage(ProxyMessageOut message) {
        if (Streamline.getInstance().getOnlinePlayers().size() <= 0) return;

        new ArrayList<>(Streamline.getInstance().getProxy().getOnlinePlayers()).get(0)
                .sendPluginMessage(Streamline.getInstance(), message.getChannel(), message.asWrite());
    }

    @Override
    public void receiveMessage(ProxyMessageEvent event) {

    }
}
