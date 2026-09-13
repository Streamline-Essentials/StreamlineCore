package singularity.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import singularity.Singularity;

/**
 * Base class for all Streamline cross-platform event listeners.
 *
 * <p>Extending {@code CosmicListener} automatically registers the subclass with the
 * TheBase {@link BaseEventHandler}, using the active {@link Singularity} instance as
 * the owning plugin context.  Subclasses annotate handler methods with TheBase's
 * {@code @BaseEventHandler} annotation to subscribe to specific {@code CosmicEvent}s.</p>
 */
public class CosmicListener implements BaseEventListener {

    /**
     * Constructs a {@code CosmicListener} and immediately registers it with the
     * {@link BaseEventHandler} bound to the current {@link Singularity} instance.
     */
    public CosmicListener() {
        BaseEventHandler.bake(this, Singularity.getInstance());
    }
}
