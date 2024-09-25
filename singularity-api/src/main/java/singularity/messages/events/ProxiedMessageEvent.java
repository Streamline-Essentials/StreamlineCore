package singularity.messages.events;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;
import singularity.messages.proxied.ProxiedMessage;

@Getter
public class ProxiedMessageEvent extends CosmicEvent {
    private final ProxiedMessage message;

    public ProxiedMessageEvent(ProxiedMessage message) {
        this.message = message;
    }

    public CosmicPlayer getCarrier() {
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
