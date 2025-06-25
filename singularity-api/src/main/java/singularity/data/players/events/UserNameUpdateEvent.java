package singularity.data.players.events;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

@Getter @Setter
public class UserNameUpdateEvent extends CosmicSenderEvent {
    private String changeTo;
    private final String changeFrom;

    public UserNameUpdateEvent(CosmicPlayer player, String changeTo, String changeFrom) {
        super(player);
        this.changeTo = changeTo;
        this.changeFrom = changeFrom;
    }
}
