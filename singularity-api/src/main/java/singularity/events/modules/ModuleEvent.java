package singularity.events.modules;

import singularity.events.CosmicEvent;
import singularity.modules.ModuleLike;
import org.jetbrains.annotations.NotNull;
import org.pf4j.ExtensionPoint;

/**
 * Abstract base for all module lifecycle events (load, enable, disable).
 *
 * <p>Also implements {@link ExtensionPoint} so that PF4J extensions can
 * listen to module events through the standard extension mechanism.</p>
 */
public abstract class ModuleEvent extends CosmicEvent implements ExtensionPoint {

    /**
     * The module that is the subject of this event.
     */
    private final ModuleLike module;

    /**
     * Constructs a {@code ModuleEvent} for the given module.
     *
     * @param module the module involved in this event; must not be {@code null}
     */
    public ModuleEvent(@NotNull final ModuleLike module) {
        super();
        this.module = module;
    }

    /**
     * Gets the module involved in this event.
     *
     * @return the module that triggered this event; never {@code null}
     */
    @NotNull
    public ModuleLike getModule() {
        return module;
    }
}
