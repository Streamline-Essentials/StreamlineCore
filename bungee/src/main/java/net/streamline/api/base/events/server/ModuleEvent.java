package net.streamline.api.base.events.server;

import net.streamline.api.base.modules.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Used for module enable and disable events
 */
public abstract class ModuleEvent extends ServerEvent {
    private final Module module;

    public ModuleEvent(@NotNull final Module module) {
        this.module = module;
    }

    /**
     * Gets the module involved in this event
     *
     * @return Module for this event
     */
    @NotNull
    public Module getModule() {
        return module;
    }
}
