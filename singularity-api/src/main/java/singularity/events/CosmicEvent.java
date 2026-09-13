package singularity.events;

import gg.drak.thebase.events.components.BaseEvent;

/**
 * Base class for all cross-platform Streamline events.
 *
 * <p>Every event that is part of the Singularity framework's cross-platform event
 * hierarchy should extend this class rather than {@link BaseEvent} directly, so
 * that consumers can reliably type-check or filter on the Streamline event layer.</p>
 */
public abstract class CosmicEvent extends BaseEvent {

    /**
     * Constructs a new {@code CosmicEvent}, delegating initialisation to
     * {@link BaseEvent}.
     */
    public CosmicEvent() {}
}
