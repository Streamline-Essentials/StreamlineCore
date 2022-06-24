package net.streamline.api.base.modules;

import net.streamline.api.base.events.Event;
import net.streamline.api.base.events.EventException;
import net.streamline.api.base.events.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface EventExecutor {
    public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException;
}
