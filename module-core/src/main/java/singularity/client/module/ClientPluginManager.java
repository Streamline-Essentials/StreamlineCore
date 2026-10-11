package singularity.client.module;

import org.pf4j.JarPluginManager;
import org.pf4j.PluginWrapper;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * PF4J's jar loader as {@link ClientModuleManager} uses it: for reading descriptors,
 * resolving dependencies and building class loaders only. The manager runs each module's
 * lifecycle itself and never starts a plugin through PF4J.
 */
final class ClientPluginManager extends JarPluginManager {

    ClientPluginManager(Path cache) {
        super(cache);
    }

    /**
     * Drops a plugin and closes its class loader, releasing the jar it was loaded from.
     * {@code unloadPlugin} cannot be used: it stops the plugin first, which creates the
     * plugin's instance, and a jar whose main class does not link (one built against classes
     * the client lacks) fails there and stays registered, blocking every later jar with its id.
     */
    void forget(String pluginId) {
        PluginWrapper wrapper = plugins.remove(pluginId);
        if (wrapper != null) {
            resolvedPlugins.remove(wrapper);
            unresolvedPlugins.remove(wrapper);
            startedPlugins.remove(wrapper);
        }
        ClassLoader loader = pluginClassLoaders.remove(pluginId);
        if (loader instanceof Closeable) {
            try {
                ((Closeable) loader).close();
            } catch (IOException e) {
                // The copy it read from is deleted when the game exits instead.
            }
        }
    }
}
