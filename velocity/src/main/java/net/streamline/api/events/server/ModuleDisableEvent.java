package net.streamline.api.events.server;

import net.streamline.api.modules.BundledModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class ModuleDisableEvent extends ModuleEvent {
    public ModuleDisableEvent(@NotNull final BundledModule module) {
        super(Boolean.class, module);
    }
}
