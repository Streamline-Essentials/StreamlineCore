package singularity.events.modules;

import singularity.modules.CosmicModule;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a {@link CosmicModule} is being disabled.
 *
 * <p>Listeners should use this event to clean up any resources or registrations
 * that were established when the module was enabled, before the module is fully
 * stopped.</p>
 */
public class ModuleDisableEvent extends RegularModuleEvent {

    /**
     * Constructs a {@code ModuleDisableEvent} for the specified module.
     *
     * @param module the module that is being disabled; must not be {@code null}
     */
    public ModuleDisableEvent(@NotNull final CosmicModule module) {
        super(module);
    }
}
