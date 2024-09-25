package singularity.data.players.events;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.messages.proxied.ProxiedMessage;

@Getter @Setter
public class UserNameUpdateByOtherEvent extends UserNameUpdateEvent {
    ProxiedMessage proxiedMessage;

    public UserNameUpdateByOtherEvent(CosmicPlayer user, String changeTo, String changeFrom, ProxiedMessage proxiedMessage) {
        super(user, changeTo, changeFrom);

        this.proxiedMessage = proxiedMessage;
    }
}
