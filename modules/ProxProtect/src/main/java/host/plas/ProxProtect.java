package host.plas;

import host.plas.configs.HazardsConfig;
import host.plas.events.ProxListener;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;
import org.pf4j.PluginWrapper;

import java.util.List;

public class ProxProtect extends SimpleModule {
    @Getter @Setter
    private static ProxProtect instance; // This will be used to access the module instance from anywhere in the plugin.

    @Getter @Setter
    private static HazardsConfig hazardsConfig; // This will be used to access the config instance from anywhere in the plugin.
    @Getter @Setter
    private static ProxListener proxListener; // This will be used to access the listener instance from anywhere in the plugin.

    public ProxProtect(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(List.of(
                // Add commands here.
        ));
    }

    @Override
    public void onEnable() {
        instance = this; // Set the instance to this module upon enabling.

        hazardsConfig = new HazardsConfig(); // Initialize the config.

        proxListener = new ProxListener(); // Initialize the listener.
        ModuleUtils.listen(proxListener, this); // Register the listener.
    }
}
