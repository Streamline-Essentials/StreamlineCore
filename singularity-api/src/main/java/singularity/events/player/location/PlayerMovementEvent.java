package singularity.events.player.location;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.events.player.PlatformedPlayerEvent;

/**
 * Fired when a {@link CosmicPlayer} moves from one location to another.
 *
 * <p>The event captures both the player's current (old) location at the time the
 * event is constructed and the intended new location.  Movement is not applied to
 * the player object until {@link #completeMovement()} is explicitly called, giving
 * listeners the opportunity to inspect or cancel the transition beforehand.</p>
 */
@Getter @Setter
public class PlayerMovementEvent extends PlatformedPlayerEvent {

    /**
     * The player who is moving.
     */
    private CosmicPlayer player;

    /**
     * The location the player is moving away from, captured at event construction time.
     */
    private CosmicLocation oldLocation;

    /**
     * The destination location the player is moving towards.
     */
    private CosmicLocation newLocation;

    /**
     * Constructs a {@code PlayerMovementEvent} for the given player and destination.
     *
     * <p>The player's current location at the time of construction is stored as
     * {@link #oldLocation}.</p>
     *
     * @param player      the player who is moving; must not be {@code null}
     * @param newLocation the destination location; must not be {@code null}
     */
    public PlayerMovementEvent(CosmicPlayer player, CosmicLocation newLocation) {
        super(player.getUuid());

        this.player = player;

        this.oldLocation = player.getLocation();
        this.newLocation = newLocation;
    }

    /**
     * Applies the movement by updating the player's stored location to
     * {@link #newLocation}.
     *
     * <p>This method should be called after all listeners have had a chance to
     * inspect the event and no listener has prevented the move.</p>
     */
    public void completeMovement() {
        player.setLocation(newLocation);
    }
}
