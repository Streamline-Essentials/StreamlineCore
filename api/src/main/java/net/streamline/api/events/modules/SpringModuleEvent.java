package net.streamline.api.events.modules;

import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

/**
 * Used for module enable and disable events
 */
public abstract class SpringModuleEvent extends ModuleEvent {
    public SpringModuleEvent(@NotNull final StreamlineSpringModule module) {
        super(module);
    }
}
