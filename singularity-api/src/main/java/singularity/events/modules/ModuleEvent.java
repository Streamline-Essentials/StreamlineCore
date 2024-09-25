package singularity.events.modules;

import singularity.events.CosmicEvent;
import singularity.modules.ModuleLike;
import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

/**
 * Used for module enable and disable events
 */
public abstract class ModuleEvent extends CosmicEvent implements ExtensionPoint {
    private final ModuleLike module;

    public ModuleEvent(@NotNull final ModuleLike module) {
        super();
        this.module = module;
    }

    /**
     * Gets the module involved in this event
     *
     * @return Module for this event
     */
    @NotNull
    public ModuleLike getModule() {
        return module;
    }
}
