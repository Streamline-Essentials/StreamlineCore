package singularity.events.player.location;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.events.player.PlatformedPlayerEvent;

@Getter @Setter
public class PlayerMovementEvent extends PlatformedPlayerEvent {
    private CosmicPlayer player;
    private CosmicLocation oldLocation;
    private CosmicLocation newLocation;

    public PlayerMovementEvent(CosmicPlayer player, CosmicLocation newLocation) {
        super(player.getUuid());

        this.player = player;

        this.oldLocation = player.getLocation();
        this.newLocation = newLocation;
    }

    public void completeMovement() {
        player.setLocation(newLocation);
    }
}
