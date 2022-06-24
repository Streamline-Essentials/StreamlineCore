package net.streamline.api.modules;

import net.streamline.api.events.*;
import org.jetbrains.annotations.NotNull;

/**
 * Stores relevant information for module listeners
 */
public class RegisteredListener {
    private final Listener listener;
    private final EventPriority priority;
    private final Module module;
    private final EventExecutor executor;
    private final boolean ignoreCancelled;

    public RegisteredListener(@NotNull final Listener listener, @NotNull final EventExecutor executor, @NotNull final EventPriority priority, @NotNull final Module module, final boolean ignoreCancelled) {
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
    public Listener getListener() {
        return listener;
    }

    /**
     * Gets the module for this registration
     *
     * @return Registered Module
     */
    @NotNull
    public Module getModule() {
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
    public void callEvent(@NotNull final Event event) throws EventException {
        if (event instanceof Cancellable) {
            if (((Cancellable) event).isCancelled() && isIgnoringCancelled()) {
                return;
            }
        }
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
