package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is loaded.
 */
public class ModuleLoadEvent extends RegularModuleEvent {
    public ModuleLoadEvent(@NotNull final StreamlineModule module) {
        super(module);
    }
}
