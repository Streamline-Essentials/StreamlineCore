package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is loaded.
 */
public class ModuleLoadEvent extends RegularModuleEvent {
    public ModuleLoadEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
