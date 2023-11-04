package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Used for module enable and disable events
 */
public abstract class RegularModuleEvent extends ModuleEvent {

    public RegularModuleEvent(@NotNull final StreamlineModule module) {
        super(module);
    }
}
