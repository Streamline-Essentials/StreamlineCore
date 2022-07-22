package net.streamline.api.events;

import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Stores relevant information for plugin listeners
 */
public class RegisteredListener {
    private final StreamlineListener listener;
    private final EventPriority priority;
    private final StreamlineModule module;
    private final EventExecutor executor;
    private final boolean ignoreCancelled;

    public RegisteredListener(@NotNull final StreamlineListener listener, @NotNull final EventExecutor executor, @NotNull final EventPriority priority, @NotNull final StreamlineModule module, final boolean ignoreCancelled) {
        this.listener = listener;
        this.priority = priority;
        this.module = module;
        this.executor = executor;
        this.ignoreCancelled = ignoreCancelled;
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
    public StreamlineModule getModule() {
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
    public void callEvent(@NotNull final StreamlineEvent<?> event) throws EventException {
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
}
