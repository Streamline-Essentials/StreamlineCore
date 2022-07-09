package net.streamline.api.events.server;

import net.streamline.api.modules.BundledModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class ModuleLoadEvent extends ModuleEvent {
    public ModuleLoadEvent(@NotNull final BundledModule module) {
        super(module);
    }
}
