package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is disabled.
 */
public class ModuleDisableEvent extends RegularModuleEvent {
    public ModuleDisableEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
