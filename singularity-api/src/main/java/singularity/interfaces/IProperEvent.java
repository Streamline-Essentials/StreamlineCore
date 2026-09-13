package singularity.interfaces;

import singularity.events.CosmicEvent;

/**
 * Bridge interface that links a platform-native event of type {@code E} to the
 * corresponding cross-platform {@link CosmicEvent}.
 *
 * <p>Platform listener adapters implement this interface so that a single handler
 * can work with both the native event object (for platform-specific operations) and
 * the Streamline-level event (for cross-platform logic).</p>
 *
 * @param <E> the platform-native event type (e.g., a Velocity or BungeeCord event class)
 */
public interface IProperEvent<E> {

    /**
     * Returns the platform-native event object associated with this proper event.
     *
     * @return the native event; never {@code null} after construction
     */
    E getEvent();

    /**
     * Returns the cross-platform {@link CosmicEvent} that corresponds to the wrapped
     * platform-native event.
     *
     * @return the Streamline-level event representation
     */
    CosmicEvent getCosmicEvent();

    /**
     * Replaces the wrapped platform-native event object.
     *
     * @param event the new platform-native event; must not be {@code null}
     */
    void setEvent(E event);

    /**
     * Replaces the associated cross-platform {@link CosmicEvent}.
     *
     * @param cosmicEvent the new Streamline-level event; must not be {@code null}
     */
    void setCosmicEvent(CosmicEvent cosmicEvent);
}
