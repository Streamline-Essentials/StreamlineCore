package host.plas;

import host.plas.commands.RunOnCommand;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.SimpleModule;
import org.pf4j.PluginWrapper;

import java.util.List;

public class RunOn extends SimpleModule {
    @Getter @Setter
    private static RunOn instance; // This will be used to access the module instance from anywhere in the plugin.

    public RunOn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(List.of(
                // Add commands here.
                new RunOnCommand()
        ));
    }

    @Override
    public void onEnable() {
        instance = this; // Set the instance to this module upon enabling.
    }
}
