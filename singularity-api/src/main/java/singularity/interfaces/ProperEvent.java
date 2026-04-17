package singularity.interfaces;

import lombok.Getter;

/**
 * Abstract base class that wraps a platform-native event of type {@code E} for use
 * within the Streamline event pipeline.
 *
 * <p>Concrete subclasses are created by platform listener adapters and passed to
 * {@link ISingularityExtension#fireEvent(IProperEvent)} so that both the native event
 * object and the corresponding {@link singularity.events.CosmicEvent} are available to
 * downstream handlers.</p>
 *
 * @param <E> the platform-native event type being wrapped
 */
@Getter
public abstract class ProperEvent<E> {

    /**
     * The platform-native event object that triggered this proper event.
     */
    private final E event;

    /**
     * Constructs a {@code ProperEvent} wrapping the given platform-native event.
     *
     * @param event the platform event to wrap; must not be {@code null}
     */
    public ProperEvent(E event) {
        this.event = event;
    }
}
