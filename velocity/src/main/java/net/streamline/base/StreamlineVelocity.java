package net.streamline.base;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;
import org.slf4j.Logger;
import singularity.modules.ModuleManager;

import java.io.File;
import java.nio.file.Path;

//@Plugin(
//        id = "streamlinecore",
//        name = "StreamlineCore",
//        dependencies = {
//                @Dependency(id = "luckperms"),
//                @Dependency(id = "geyser-velocity", optional = true)
//        }
//)
/**
 * Velocity entry-point for the StreamlineCore plugin.
 *
 * <p>This class is injected by Velocity's dependency-injection framework and extends
 * {@link BasePlugin} with Velocity-specific startup logic: it registers bStats custom
 * charts on {@link #enable()}, stops all PF4J modules on {@link #disable()}, and
 * provides static helpers for resolving the Velocity plugins directory and reading
 * build-time properties injected by Gradle.
 */
public class StreamlineVelocity extends BasePlugin {
    /**
     * Constructs the plugin via Velocity dependency injection.
     *
     * @param server         the Velocity {@link ProxyServer} instance
     * @param logger         the SLF4J logger provided by Velocity
     * @param metricsFactory the bStats {@link Metrics.Factory} for registering charts
     */
    @Inject
    public StreamlineVelocity(ProxyServer server,
                              Logger logger,
                              Metrics.Factory metricsFactory) {
        super(server, logger, getOwnFolder(), metricsFactory);
    }

    /**
     * Returns the plugin's own data folder ({@code plugins/StreamlineCore}).
     *
     * @return the {@link File} representing this plugin's data directory
     */
    public static File getOwnFolder() {
        return new File(getPluginsDirectory(), "StreamlineCore");
    }

    /**
     * Locates the Velocity {@code plugins} directory by searching the working directory
     * and its parent for a child named {@code "plugins"}.
     *
     * @return the {@link File} pointing to the {@code plugins} directory, or {@code null}
     *         if it could not be found
     */
    public static File getPluginsDirectory() {
        File file = getSystemFile();

        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }

        File pluginDirectory = null;
        for (File f : files) {
            if (f.getName().equals("plugins")) {
                pluginDirectory = f;
                break;
            }
        }
        if (pluginDirectory == null) {
            file = file.getParentFile();

            files = file.listFiles();
            if (files == null) {
                return null;
            }

            for (File f : files) {
                if (f.getName().equals("plugins")) {
                    pluginDirectory = f;
                    break;
                }
            }
        }

        return pluginDirectory;
    }

    /**
     * Returns the JVM's current working directory as a {@link Path}.
     *
     * @return a {@link Path} derived from the {@code user.dir} system property
     */
    public static Path getSystemPath() {
        return Path.of(System.getProperty("user.dir"));
    }

    /**
     * Returns the JVM's current working directory as a {@link File}.
     *
     * @return a {@link File} derived from {@link #getSystemPath()}
     */
    public static File getSystemFile() {
        return getSystemPath().toFile();
    }

    /**
     * Returns the plugin's name as injected by Gradle at build time.
     *
     * <p>Falls back to {@code "StreamlineCore"} when the template variable has not
     * been substituted (e.g., during IDE runs).
     *
     * @return the plugin name string
     */
    public static String getStreamlineName() {
        String name = "${name}"; // Gets injected by Gradle

        if (name.startsWith("$")) {
            name = "StreamlineCore";
        }

        return name;
    }

    /**
     * Returns the plugin's version as injected by Gradle at build time.
     *
     * <p>Falls back to {@code "2.5.2.0"} when the template variable has not been
     * substituted (e.g., during IDE runs).
     *
     * @return the plugin version string
     */
    public static String getStreamlineVersion() {
        String version = "${version}"; // Gets injected by Gradle

        if (version.startsWith("$")) {
            version = "2.5.2.0";
        }

        return version;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initialises bStats metrics with charts reporting the plugin version and the count
     * of loaded and enabled PF4J modules, then marks the platform as enabled.
     */
    @Override
    public void enable() {
        Metrics metrics = getMetricsFactory().make(this, 26274);
        metrics.addCustomChart(new Metrics.SimplePie("plugin_version", () -> getProxy().getPluginManager().getPlugin("streamlinecore").get().getDescription().getVersion().get()));
        metrics.addCustomChart(new Metrics.SimplePie("modules_loaded_count", () -> String.valueOf(ModuleManager.getLoadedModules().size())));
        metrics.addCustomChart(new Metrics.SimplePie("modules_enabled_count", () -> String.valueOf(ModuleManager.getEnabledModules().size())));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_loaded", () -> ModuleManager.getLoadedModules().size()));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_enabled", () -> ModuleManager.getEnabledModules().size()));

        setPlatformAsEnabled();
    }

//    public Map<String, Integer> getInstalledModulesCount() {
//        Map<String, Integer> map = new HashMap<>();
//
//        return map;
//    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops all loaded PF4J modules via {@link ModuleManager#stopModules()}.
     */
    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Currently a no-op; pre-enable load logic is handled by {@link BasePlugin}.
     */
    @Override
    public void load() {

    }
}