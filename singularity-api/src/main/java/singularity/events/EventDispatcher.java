package singularity.events;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.RegisteredListener;
import gg.drak.thebase.events.components.BaseEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fires events to TheBase listeners in {@link gg.drak.thebase.events.processing.BaseProcessor#priority()
 * priority} order: {@code LOWEST} first, {@code MONITOR} last, and listeners of one priority in
 * the order they registered.
 *
 * <p>TheBase's own {@link BaseEventHandler#fireEvent(BaseEvent)} calls listeners in registration
 * order only and ignores their priority, so which module saw an event first would depend on which
 * loaded first. Everything Streamline fires goes through here instead.</p>
 */
public final class EventDispatcher {
    private static final Comparator<RegisteredListener<?>> BY_PRIORITY =
            Comparator.comparingInt(listener -> listener.getPriority().getSlot());

    private EventDispatcher() {
    }

    public static void fire(BaseEvent event) {
        if (event == null) return;

        // getRegularListeners is in registration order and List.sort is stable, so ties keep it.
        List<RegisteredListener<?>> listeners = new ArrayList<>(BaseEventHandler.getRegularListeners(event.getClass()));
        listeners.sort(BY_PRIORITY);
        for (RegisteredListener<?> listener : listeners) {
            try {
                listener.callEvent(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        BaseEventHandler.getFunctions().forEach(function -> {
            if (function.getClazz().isAssignableFrom(event.getClass())) function.fire(event);
        });
    }
}
