package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Stores relevant information for plugin listeners
 */
public class RegisteredListener<T extends ModuleLike> implements Comparable<RegisteredListener<T>> {
    @Getter @Setter
    private static int masterIndex = 0;

    private final StreamlineListener listener;
    private final EventPriority priority;
    private final T module;
    private final EventExecutor executor;
    private final boolean ignoreCancelled;
    private final int index;

    public RegisteredListener(@NotNull final StreamlineListener listener, @NotNull final EventExecutor executor, @NotNull final EventPriority priority, @NotNull final T module, final boolean ignoreCancelled) {
        this.listener = listener;
        this.priority = priority;
        this.module = module;
        this.executor = executor;
        this.ignoreCancelled = ignoreCancelled;
        setMasterIndex(getMasterIndex() + 1);
        this.index = getMasterIndex();
    }

    /**
     * Gets the listener for this registration
     *
     * @return Registered Listener
     */
    @NotNull
    public StreamlineListener getListener() {
        return listener;
    }

    /**
     * Gets the plugin for this registration
     *
     * @return Registered Plugin
     */
    @NotNull
    public T getModule() {
        return module;
    }

    /**
     * Gets the priority for this registration
     *
     * @return Registered Priority
     */
    @NotNull
    public EventPriority getPriority() {
        return priority;
    }

    /**
     * Calls the event executor
     *
     * @param event The event
     * @throws EventException If an event handler throws an exception.
     */
    public void callEvent(@NotNull final StreamlineEvent event) throws EventException {
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
