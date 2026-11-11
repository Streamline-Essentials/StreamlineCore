package gg.drak.thebase.events.processing;

import org.jetbrains.annotations.NotNull;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.components.BaseEvent;
import gg.drak.thebase.events.processing.exception.BaseEventException;

/**
 * Interface which defines the class for event call backs to plugins
 */
public interface BaseEventExecutor {
    public void execute(@NotNull BaseEventListener listener, @NotNull BaseEvent event) throws BaseEventException;
}
