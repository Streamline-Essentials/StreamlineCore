package gg.drak.thebase.events;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import gg.drak.thebase.events.components.BaseEvent;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.exception.BaseEventException;
import gg.drak.thebase.events.processing.BaseEventExecutor;
import gg.drak.thebase.objects.handling.IEventable;

/**
 * Stores relevant information for plugin listeners
 */
public class RegisteredListener<T extends IEventable> implements Comparable<RegisteredListener<T>> {
    @Getter @Setter
    private static int masterIndex = 0;

    private final BaseEventListener listener;
    private final BaseEventPriority priority;
    private final BaseEventExecutor executor;
    private final boolean ignoreCancelled;
    private final int index;
    private final T eventable;

    public RegisteredListener(@NotNull final BaseEventListener listener, @NotNull final BaseEventExecutor executor, @NotNull final BaseEventPriority priority, T eventable, final boolean ignoreCancelled) {
        this.listener = listener;
        this.priority = priority;
        this.executor = executor;
        this.ignoreCancelled = ignoreCancelled;
        setMasterIndex(getMasterIndex() + 1);
        this.index = getMasterIndex();
        this.eventable = eventable;
    }

    /**
     * Gets the listener for this registration
     *
     * @return Registered Listener
     */
    @NotNull
    public BaseEventListener getListener() {
        return listener;
    }

    /**
     * Gets the listener for this registration
     *
     * @return Registered Listener
     */
    @NotNull
    public T getEventable() {
        return eventable;
    }

    /**
     * Gets the priority for this registration
     *
     * @return Registered Priority
     */
    @NotNull
    public BaseEventPriority getPriority() {
        return priority;
    }

    /**
     * Calls the event executor
     *
     * @param event The event
     * @throws BaseEventException If an event handler throws an exception.
     */
    public void callEvent(@NotNull final BaseEvent event) throws BaseEventException {
        executor.execute(listener, event);
    }

    /**
     * Whether this listener accepts cancelled events
     *
     * @return True when ignoring cancelled events
     */
    public boolean isIgnoringCancelled() {
        return ignoreCancelled;
    }

    @Override
    public int compareTo(@NotNull RegisteredListener o) {
        return Integer.compare(index, o.index);
    }
}
