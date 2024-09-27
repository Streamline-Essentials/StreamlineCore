package net.streamline.base;

import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.Setter;
import net.streamline.metrics.Metrics;
import net.streamline.platform.BasePlugin;
import org.slf4j.Logger;
import singularity.modules.ModuleManager;

import javax.inject.Inject;
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
    public StreamlineVelocity(ProxyServer s, Logger l, @DataDirectory Path dd, Metrics.Factory mf) {
        super(s, l, dd, mf);
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

        Metrics metrics = getMetricsFactory().make(this, 16971);
        metrics.addCustomChart(new Metrics.SimplePie("plugin_version", () -> getProxy().getPluginManager().getPlugin("streamlinecore").get().getDescription().getVersion().get()));
        metrics.addCustomChart(new Metrics.SimplePie("modules_loaded_count", () -> String.valueOf(ModuleManager.getLoadedModules().size())));
        metrics.addCustomChart(new Metrics.SimplePie("modules_enabled_count", () -> String.valueOf(ModuleManager.getEnabledModules().size())));
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