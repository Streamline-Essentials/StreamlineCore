package net.streamline.base;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleManager;
import net.streamline.base.runnables.ServerPusher;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;

public class Streamline extends BasePlugin {
    @Getter @Setter
    private static ServerPusher serverPusher;

    @Override
    public void enable() {
        try {
            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
            setServerPusher(new ServerPusher());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Metrics metrics = new Metrics(this, 16973);
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
}
