package net.streamline.api.events;

import org.jetbrains.annotations.NotNull;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface EventExecutor {
    public void execute(@NotNull StreamlineListener listener, @NotNull StreamlineEvent event) throws EventException;
}
