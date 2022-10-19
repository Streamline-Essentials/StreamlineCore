package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.savables.users.StreamlineUser;

public class UserNameUpdateByOtherEvent extends UserNameUpdateEvent {
    @Getter @Setter
    ProxiedMessage proxiedMessage;

    public UserNameUpdateByOtherEvent(StreamlineUser user, String changeTo, String changeFrom, ProxiedMessage message) {
        super(user, changeTo, changeFrom);
        setProxiedMessage(proxiedMessage);
    }
}
