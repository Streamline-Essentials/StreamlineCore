package singularity.events.player;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;
import singularity.data.players.location.CosmicLocation;

/**
 * Fired on backend servers when a player dies, before they respawn. Informational only:
 * cancelling it does not prevent the death.
 */
@Getter
public class CosmicDeathEvent extends CosmicSenderEvent {

    /** Where the player died, with the platform's world name. */
    private final CosmicLocation location;

    /**
     * @param player   the player who died
     * @param location where they died
     */
    public CosmicDeathEvent(CosmicPlayer player, CosmicLocation location) {
        super(player);
        this.location = location;
    }

    /**
     * @return the player who died
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) getSender();
    }
}
