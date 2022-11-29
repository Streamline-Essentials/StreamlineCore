package net.streamline.base;

import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;
import net.streamline.api.modules.ModuleManager;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        try {
            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Metrics metrics = new Metrics(this, 16972);
        metrics.addCustomChart(new Metrics.SimplePie("plugin_version", () -> getDescription().getVersion()));
        metrics.addCustomChart(new Metrics.SimplePie("modules_loaded_count", () -> String.valueOf(ModuleManager.getLoadedModules().size())));
        metrics.addCustomChart(new Metrics.SimplePie("modules_enabled_count", () -> String.valueOf(ModuleManager.getEnabledModules().size())));
    }

    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    @Override
    public void load() {

    }

    @Override
    public void reload() {

    }
}
