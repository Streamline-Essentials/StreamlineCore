package singularity.modules;

import org.pf4j.PluginWrapper;

/**
 * A no-op convenience subclass of {@link CosmicModule} for modules that do
 * not need custom enable, disable, or command-registration logic.
 *
 * <p>All lifecycle methods ({@link #onEnable()}, {@link #onDisable()}, and
 * {@link #registerCommands()}) have empty default implementations so that
 * concrete subclasses can override only the hooks they actually use.
 */
public abstract class SimpleModule extends CosmicModule {

    /**
     * Constructs a {@code SimpleModule} by delegating to the
     * {@link CosmicModule} constructor, which sets up the data folder,
     * registers the module, and fires {@link #onLoad()}.
     *
     * @param wrapper the PF4J plugin wrapper supplied by the plugin manager
     */
    public SimpleModule(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation is a no-op; override to add enable logic.
     */
    @Override
    public void onEnable() {

    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation is a no-op; override to add disable logic.
     */
    @Override
    public void onDisable() {

    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation is a no-op; override to register commands
     * via {@link #addCommand(singularity.command.ModuleCommand)}.
     */
    @Override
    public void registerCommands() {

    }
}
