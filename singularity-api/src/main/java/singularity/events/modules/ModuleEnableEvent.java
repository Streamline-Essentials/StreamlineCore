package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is enabled.
 */
public class ModuleEnableEvent extends RegularModuleEvent {
    public ModuleEnableEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
