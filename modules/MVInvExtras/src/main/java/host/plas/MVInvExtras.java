package host.plas;

import host.plas.commands.ReloadCommand;
import host.plas.configs.MainConfig;
import host.plas.events.MainListener;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.modules.SimpleModule;

import java.util.ArrayList;
import java.util.List;

public class MVInvExtras extends SimpleModule {
    @Getter @Setter
    private static MVInvExtras instance; // This will be used to access the module instance from anywhere in the plugin.

    @Getter @Setter
    private static MainConfig exampleConfig; // This will be used to access the config instance from anywhere in the plugin.
    @Getter @Setter
    private static MainListener mainListener; // This will be used to access the listener instance from anywhere in the plugin.

    public MVInvExtras(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(new ArrayList<>(List.of(
                new ReloadCommand()
        )));
    }

    @Override
    public void onEnable() {
        instance = this; // Set the instance to this module upon enabling.

        exampleConfig = new MainConfig(); // Initialize the config.

        mainListener = new MainListener(); // Initialize the listener.
    }
}
