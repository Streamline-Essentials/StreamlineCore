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

@Plugin(
        id = "streamlinecore",
        name = "${name}",
        version = "${version}",
        dependencies = {
                @Dependency(id = "luckperms"),
                @Dependency(id = "geyser-velocity", optional = true)
        }
)
public class StreamlineVelocity extends BasePlugin {
    @Inject
    public StreamlineVelocity(ProxyServer server,
                              Logger logger,
                              Metrics.Factory metricsFactory) {
        super(server, logger, getStreamlineFolder(), metricsFactory);
    }

    public static File getStreamlineFolder() {
        return new File(getPluginsDirectory(), "StreamlineCore");
    }

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

    public static Path getSystemPath() {
        return Path.of(System.getProperty("user.dir"));
    }

    public static File getSystemFile() {
        return getSystemPath().toFile();
    }

    public static String getStreamlineName() {
        String name = "${name}"; // Gets injected by Gradle

        if (name.startsWith("$")) {
            name = "StreamlineCore";
        }

        return name;
    }

    public static String getStreamlineVersion() {
        String version = "${version}"; // Gets injected by Gradle

        if (version.startsWith("$")) {
            version = "2.5.2.0";
        }

        return version;
    }

    @Override
    public void enable() {
        try {
            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
//            setServerPusher(new ServerPusher());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Metrics metrics = getMetricsFactory().make(this, 26274);
        metrics.addCustomChart(new Metrics.SimplePie("plugin_version", () -> getProxy().getPluginManager().getPlugin("streamlinecore").get().getDescription().getVersion().get()));
        metrics.addCustomChart(new Metrics.SimplePie("modules_loaded_count", () -> String.valueOf(ModuleManager.getLoadedModules().size())));
        metrics.addCustomChart(new Metrics.SimplePie("modules_enabled_count", () -> String.valueOf(ModuleManager.getEnabledModules().size())));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_loaded", () -> ModuleManager.getLoadedModules().size()));
        metrics.addCustomChart(new Metrics.SingleLineChart("total_modules_enabled", () -> ModuleManager.getEnabledModules().size()));
    }

//    public Map<String, Integer> getInstalledModulesCount() {
//        Map<String, Integer> map = new HashMap<>();
//
//        return map;
//    }

    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    @Override
    public void load() {

    }
}