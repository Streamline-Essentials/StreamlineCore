package net.streamline.base;

import lombok.Getter;
import lombok.Setter;
import net.streamline.base.runnables.ServerPusher;
import singularity.modules.ModuleManager;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;

/**
 * The BungeeCord platform entry point for StreamlineCore.
 *
 * <p>Extends {@link BasePlugin} to provide BungeeCord-specific startup,
 * shutdown, and bStats metrics registration. A {@link ServerPusher} periodic
 * task is created during enable to drive server-push operations.
 */
public class StreamlineBungee extends BasePlugin {

    /**
     * The periodic runnable responsible for pushing server data on the
     * BungeeCord platform.
     */
    @Getter @Setter
    private static ServerPusher serverPusher;

    /**
     * Called when the BungeeCord plugin is enabled.
     *
     * <p>Registers bStats metrics charts for plugin version, loaded module
     * count, and enabled module count, then marks the platform as enabled.
     */
    @Override
    public void enable() {
        Metrics metrics = new Metrics(this, 26272);
        metrics.addCustomChart(new Metrics.SimplePie("plugin_version", () -> getDescription().getVersion()));
        metrics.addCustomChart(new Metrics.SimplePie("modules_loaded_count", () -> String.valueOf(ModuleManager.getLoadedModules().size())));
        metrics.addCustomChart(new Metrics.SimplePie("modules_enabled_count", () -> String.valueOf(ModuleManager.getEnabledModules().size())));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_loaded", () -> ModuleManager.getLoadedModules().size()));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_enabled", () -> ModuleManager.getEnabledModules().size()));

        setPlatformAsEnabled();
    }

    /**
     * Called when the BungeeCord plugin is disabled.
     *
     * <p>Stops all loaded modules via {@link ModuleManager#stopModules()}.
     */
    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    /**
     * Called during the BungeeCord plugin load phase.
     *
     * <p>This implementation is intentionally empty; all initialisation
     * occurs in {@link #enable()}.
     */
    @Override
    public void load() {

    }
}
