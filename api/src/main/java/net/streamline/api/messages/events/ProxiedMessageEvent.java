package net.streamline.api.messages.events;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.savables.users.StreamlinePlayer;

public class ProxiedMessageEvent extends StreamlineEvent {
    @Getter
    private final ProxiedMessage message;

    public ProxiedMessageEvent(ProxiedMessage message) {
        this.message = message;
    }

    public StreamlinePlayer getCarrier() {
        return message.getCarrier();
    }

    public boolean isProxyOriginated() {
        return message.isProxyOriginated();
    }

    public String getSubChannel() {
        return message.getSubChannel();
    }

    public boolean hasKey(String key) {
        return message.hasKey(key);
    }

    public String getString(String key) {
        return message.getString(key);
    }
}
