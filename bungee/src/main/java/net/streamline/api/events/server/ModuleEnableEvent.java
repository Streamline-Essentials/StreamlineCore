package net.streamline.api.events.server;

import net.streamline.api.events.HandlerList;
import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a module is enabled.
 */
public class ModuleEnableEvent extends ModuleEvent {
    private static final HandlerList handlers = new HandlerList();

    public ModuleEnableEvent(@NotNull final Module module) {
        super(module);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
