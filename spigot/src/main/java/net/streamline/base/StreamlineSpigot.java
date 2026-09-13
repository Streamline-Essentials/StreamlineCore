package net.streamline.base;

import lombok.Getter;
import lombok.Setter;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.commands.StreamlineSpigotCommand;
import singularity.modules.ModuleManager;

/**
 * Main entry point for the StreamlineCore Spigot platform plugin.
 *
 * <p>Extends {@link BasePlugin} with Spigot-specific lifecycle hooks:
 * registers bStats metrics charts and the {@link StreamlineSpigotCommand}
 * during {@link #enable()}, and cleanly stops all PF4J modules during
 * {@link #disable()}.
 */
public class StreamlineSpigot extends BasePlugin {
    /**
     * The root Streamline command registered on the Spigot server, used to
     * dispatch sub-commands to the cross-platform command handler.
     */
    @Getter @Setter
    private static StreamlineSpigotCommand streamlineSpigotCommand;

    /**
     * {@inheritDoc}
     *
     * <p>Registers bStats metrics charts (plugin version, loaded/enabled module
     * counts), creates the {@link StreamlineSpigotCommand}, and marks the
     * platform as fully enabled.
     */
    @Override
    public void enable() {
        Metrics metrics = new Metrics(this, 26273);
        metrics.addCustomChart(new Metrics.SimplePie("plugin_version", () -> getDescription().getVersion()));
        metrics.addCustomChart(new Metrics.SimplePie("modules_loaded_count", () -> String.valueOf(ModuleManager.getLoadedModules().size())));
        metrics.addCustomChart(new Metrics.SimplePie("modules_enabled_count", () -> String.valueOf(ModuleManager.getEnabledModules().size())));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_loaded", () -> ModuleManager.getLoadedModules().size()));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_enabled", () -> ModuleManager.getEnabledModules().size()));

        streamlineSpigotCommand = new StreamlineSpigotCommand();

        setPlatformAsEnabled();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops all loaded PF4J modules via {@link singularity.modules.ModuleManager#stopModules()}.
     */
    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    /** {@inheritDoc} */
    @Override
    public void load() {

    }

    /** {@inheritDoc} */
    @Override
    public void reload() {

    }
}
