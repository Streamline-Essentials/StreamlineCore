package net.streamline.base;

import lombok.Getter;
import lombok.Setter;
import net.streamline.base.runnables.ServerPusher;
import singularity.modules.ModuleManager;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;

public class StreamlineBungee extends BasePlugin {
    @Getter @Setter
    private static ServerPusher serverPusher;

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

    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    @Override
    public void load() {

    }
}
