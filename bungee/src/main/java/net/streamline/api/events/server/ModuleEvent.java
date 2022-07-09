package net.streamline.api.events.server;

import net.streamline.api.modules.BundledModule;
import org.jetbrains.annotations.NotNull;

/**
 * Used for module enable and disable events
 */
public abstract class ModuleEvent extends ServerEvent {
    private final BundledModule module;

    public ModuleEvent(@NotNull final BundledModule module) {
        this.module = module;
    }

    /**
     * Gets the module involved in this event
     *
     * @return Module for this event
     */
    @NotNull
    public BundledModule getModule() {
        return module;
    }
}
