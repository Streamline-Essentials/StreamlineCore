package net.streamline.api.modules;

import net.streamline.api.events.Event;
import net.streamline.api.events.EventException;
import net.streamline.api.events.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface EventExecutor {
    public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException;
}
