package net.streamline.api.events.server;

import net.streamline.api.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class ModuleDisableEvent extends ModuleEvent {
    public ModuleDisableEvent(@NotNull final Module module) {
        super(module);
    }
}
