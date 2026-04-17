package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a {@link CosmicModule} has been successfully enabled and is ready to
 * handle requests.
 *
 * <p>Listeners may use this event to register resources or establish integrations
 * that depend on the module being fully active.</p>
 */
public class ModuleEnableEvent extends RegularModuleEvent {

    /**
     * Constructs a {@code ModuleEnableEvent} for the specified module.
     *
     * @param module the module that was just enabled; must not be {@code null}
     */
    public ModuleEnableEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
