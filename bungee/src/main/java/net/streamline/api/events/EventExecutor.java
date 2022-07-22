package net.streamline.api.events;

import net.streamline.api.modules.ModuleUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Interface which defines the class for event call backs to plugins
 */
public class EventExecutor {
    public void execute(@NotNull StreamlineListener listener, @NotNull StreamlineEvent<?> event) throws EventException {
        ModuleUtils.fireEvent(event);
    }
}
