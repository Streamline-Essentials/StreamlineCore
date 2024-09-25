package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Used for module enable and disable events
 */
public abstract class RegularModuleEvent extends ModuleEvent {

    public RegularModuleEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
