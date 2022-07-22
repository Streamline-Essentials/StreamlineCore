package net.streamline.api.events.modules;

import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is enabled.
 */
public class ModuleEnableEvent extends ModuleEvent<Boolean> {
    public ModuleEnableEvent(@NotNull final StreamlineModule module) {
        super(module);
    }
}
