package net.streamline.api.events.modules;

import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;

/**
 * Used for module enable and disable events
 */
public abstract class ModuleEvent<T> extends StreamlineEvent<T> {
    private final StreamlineModule module;

    public ModuleEvent(@NotNull final StreamlineModule module) {
        super();
        this.module = module;
    }

    /**
     * Gets the module involved in this event
     *
     * @return Module for this event
     */
    @NotNull
    public StreamlineModule getModule() {
        return module;
    }
}
