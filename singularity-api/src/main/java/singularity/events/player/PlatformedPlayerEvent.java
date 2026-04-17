package singularity.events.player;

import lombok.Getter;
import singularity.events.CosmicEvent;

/**
 * Base class for cross-platform player events that are identified by a player UUID
 * rather than a platform-specific player object.
 *
 * <p>Using a UUID rather than a direct player reference ensures that events can be
 * created and processed uniformly across Velocity, BungeeCord, and Spigot without
 * introducing platform-specific dependencies into the event hierarchy.</p>
 */
@Getter
public class PlatformedPlayerEvent extends CosmicEvent {

    /**
     * The UUID string of the player involved in this event.
     */
    final String playerUuid;

    /**
     * Constructs a {@code PlatformedPlayerEvent} for the player with the given UUID.
     *
     * @param playerUuid the UUID of the involved player; must not be {@code null}
     */
    public PlatformedPlayerEvent(String playerUuid) {
        this.playerUuid = playerUuid;
    }
}
