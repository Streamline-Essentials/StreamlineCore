package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class SpringModuleDisableEvent extends SpringModuleEvent {
    public SpringModuleDisableEvent(@NotNull final StreamlineSpringModule module) {
        super(module);
    }
}
