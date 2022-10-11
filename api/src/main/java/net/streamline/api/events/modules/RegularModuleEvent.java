package net.streamline.api.events.modules;

import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

/**
 * Used for module enable and disable events
 */
public abstract class RegularModuleEvent extends ModuleEvent {

    public RegularModuleEvent(@NotNull final StreamlineModule module) {
        super(module);
    }
}
