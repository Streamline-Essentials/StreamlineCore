package singularity.client.module;

import org.pf4j.JarPluginLoader;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginLoader;
import org.pf4j.PluginWrapper;
import singularity.loading.ModuleClassTransformer;
import singularity.loading.TransformingPluginClassLoader;

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
     * Loads each jar through {@link ClientModuleManager#getClassTransformer()} when one is set,
     * read per jar so it applies to every module loaded after it is installed.
     */
    @Override
    protected PluginLoader createPluginLoader() {
        return new JarPluginLoader(this) {
            @Override
            public ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor) {
                ModuleClassTransformer transformer = ClientModuleManager.getClassTransformer();
                if (transformer == null) return super.loadPlugin(pluginPath, pluginDescriptor);

                PluginClassLoader loader = new TransformingPluginClassLoader(
                        pluginManager, pluginDescriptor, ClientPluginManager.class.getClassLoader(), transformer);
                loader.addFile(pluginPath.toFile());
                return loader;
            }
        };
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
