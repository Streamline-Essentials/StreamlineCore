package net.streamline.api.events.modules;

import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.ModuleLike;
import org.jetbrains.annotations.NotNull;
import tv.quaint.thebase.lib.pf4j.ExtensionPoint;

/**
 * Used for module enable and disable events
 */
public abstract class ModuleEvent extends StreamlineEvent implements ExtensionPoint {
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
