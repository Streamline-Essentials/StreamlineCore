package host.plas;

import host.plas.bukkit.GriefListener;
import host.plas.commands.MobGriefCommand;
import host.plas.config.GriefConfig;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;

import java.util.ArrayList;
import java.util.List;

public class MobGriefControl extends SimpleModule {
    @Getter @Setter
    private static MobGriefControl instance;

    @Getter @Setter
    private static GriefConfig griefConfig;

    /**
     * Held as {@link Object} so this class never links against Bukkit: the module also
     * loads on proxies and mod loaders, where {@link GriefListener} cannot be resolved.
     */
    private Object listener;

    public MobGriefControl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(new ArrayList<>(List.of(
                new MobGriefCommand()
        )));
    }

    @Override
    public void onEnable() {
        instance = this;

        griefConfig = new GriefConfig();

        if (ModuleUtils.getPlatformType() == ISingularityExtension.PlatformType.SPIGOT) {
            listener = new GriefListener();
        } else {
            logWarning("Mob grief control only works on Spigot/Paper servers; "
                    + ModuleUtils.getPlatformType() + " is not supported, so nothing will be blocked here.");
        }
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            ((GriefListener) listener).unregister();
            listener = null;
        }
    }
}
