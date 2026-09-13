package singularity.command.plugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * A PF4J {@link Plugin} subclass that serves as the base for Streamline command
 * modules. Concrete command module JARs should extend this class so that PF4J can
 * discover and manage them through the standard plugin lifecycle.
 */
public class CommandModule extends Plugin {

    /**
     * Creates a new {@code CommandModule} bound to the given PF4J plugin wrapper.
     *
     * @param wrapper the PF4J wrapper that describes this plugin's metadata and
     *                class loader
     */
    public CommandModule(PluginWrapper wrapper) {
        super(wrapper);
    }
}
