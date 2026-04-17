package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by the {@code ModuleManager} immediately after a {@link CosmicModule} has
 * been loaded from its JAR file but before it has been enabled.
 *
 * <p>Listeners can use this event to react to module discovery without waiting
 * for the module to become fully active.</p>
 */
public class ModuleLoadEvent extends RegularModuleEvent {

    /**
     * Constructs a {@code ModuleLoadEvent} for the specified module.
     *
     * @param module the module that was just loaded; must not be {@code null}
     */
    public ModuleLoadEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
