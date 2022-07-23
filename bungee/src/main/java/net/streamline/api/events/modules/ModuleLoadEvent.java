package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class ModuleLoadEvent extends ModuleEvent {
    public ModuleLoadEvent(@NotNull final StreamlineModule module) {
        super(module);
    }
}
