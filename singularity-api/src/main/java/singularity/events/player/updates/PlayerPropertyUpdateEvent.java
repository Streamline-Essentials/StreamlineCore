package singularity.events.player.updates;

import lombok.Getter;
import lombok.Setter;
import singularity.events.player.PlatformedPlayerEvent;

/**
 * Generic event fired when a player's property is about to be updated to a new value.
 *
 * <p>The type parameter {@code T} represents the type of the property being changed,
 * allowing type-safe specialisations (such as
 * {@code PlayerPropertyUpdateEvent<InetAddress>} for IP changes) without duplicating
 * the surrounding event plumbing.</p>
 *
 * @param <T> the type of the property value being updated
 */
@Getter @Setter
public class PlayerPropertyUpdateEvent<T> extends PlatformedPlayerEvent {

    /**
     * The new value that the property will be set to.
     */
    private T toSet;

    /**
     * Constructs a {@code PlayerPropertyUpdateEvent} for the given player and new value.
     *
     * @param playerUuid the UUID of the player whose property is being updated;
     *                   must not be {@code null}
     * @param toSet      the new value to assign to the property
     */
    public PlayerPropertyUpdateEvent(String playerUuid, T toSet) {
        super(playerUuid);
        this.toSet = toSet;
    }
}
