package net.streamline.api.messages.events;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.savables.users.StreamlinePlayer;

@Getter
public class ProxiedMessageEvent extends StreamlineEvent {
    private final ProxiedMessage message;

    public ProxiedMessageEvent(ProxiedMessage message) {
        this.message = message;
    }

    public StreamlinePlayer getCarrier() {
        if (message == null) return null;
        return message.getCarrier();
    }

    public boolean isProxyOriginated() {
        if (message == null) return false;
        return message.isProxyOriginated();
    }

    public String getSubChannel() {
        if (message == null) return null;
        return message.getSubChannel();
    }

    public boolean hasKey(String key) {
        if (message == null) return false;
        return message.hasKey(key);
    }

    public String getString(String key) {
        if (message == null) return null;
        return message.getString(key);
    }
}
