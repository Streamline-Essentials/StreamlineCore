package net.streamline.api.events.server;

import net.streamline.api.modules.BundledModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is enabled.
 */
public class ModuleEnableEvent extends ModuleEvent {
    public ModuleEnableEvent(@NotNull final BundledModule module) {
        super(module);
    }
}
