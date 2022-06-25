package net.streamline.api.events.server;

import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is enabled.
 */
public class ModuleEnableEvent extends ModuleEvent {
    public ModuleEnableEvent(@NotNull final Module module) {
        super(module);
    }
}
