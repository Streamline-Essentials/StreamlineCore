package net.streamline.api.data.players.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.proxied.ProxiedMessage;

@Getter @Setter
public class UserNameUpdateByOtherEvent extends UserNameUpdateEvent {
    ProxiedMessage proxiedMessage;

    public UserNameUpdateByOtherEvent(StreamPlayer user, String changeTo, String changeFrom, ProxiedMessage proxiedMessage) {
        super(user, changeTo, changeFrom);

        this.proxiedMessage = proxiedMessage;
    }
}
