package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is enabled.
 */
public class SpringModuleEnableEvent extends SpringModuleEvent {
    public SpringModuleEnableEvent(@NotNull final StreamlineSpringModule module) {
        super(module);
    }
}
