package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class ModuleDisableEvent extends ModuleEvent<Boolean> {
    public ModuleDisableEvent(@NotNull final StreamlineModule module) {
        super(module);
    }
}
