package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Intermediate base class for module lifecycle events that involve a concrete
 * {@link CosmicModule} (as opposed to the more general {@code ModuleLike} interface
 * accepted by {@link ModuleEvent}).
 *
 * <p>Load, enable, and disable events all extend this class so that listeners
 * can safely cast {@link #getModule()} to {@link CosmicModule}.</p>
 */
public abstract class RegularModuleEvent extends ModuleEvent {

    /**
     * Constructs a {@code RegularModuleEvent} for the given {@link CosmicModule}.
     *
     * @param module the concrete module involved in this event; must not be {@code null}
     */
    public RegularModuleEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
