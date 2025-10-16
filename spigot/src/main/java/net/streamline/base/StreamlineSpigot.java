package net.streamline.base;

import lombok.Getter;
import lombok.Setter;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.commands.StreamlineSpigotCommand;
import singularity.modules.ModuleManager;

public class StreamlineSpigot extends BasePlugin {
    @Getter @Setter
    private static StreamlineSpigotCommand streamlineSpigotCommand;

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
