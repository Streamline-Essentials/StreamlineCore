package singularity.events.player;

import lombok.Getter;
import singularity.events.CosmicEvent;

@Getter
public class PlatformedPlayerEvent extends CosmicEvent {
    final String playerUuid;

    public PlatformedPlayerEvent(String playerUuid) {
        this.playerUuid = playerUuid;
    }
}
