package host.plas;

import gg.drak.thebase.events.BaseEventHandler;
import host.plas.commands.MobGriefCommand;
import host.plas.config.GriefConfig;
import host.plas.events.GriefListener;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class MobGriefControl extends SimpleModule {
    /** The platforms that fire the {@code singularity.events.entity} events this module relies on. */
    private static final Set<ISingularityExtension.PlatformType> SUPPORTED = EnumSet.of(
            ISingularityExtension.PlatformType.SPIGOT,
            ISingularityExtension.PlatformType.FORGE,
            ISingularityExtension.PlatformType.NEOFORGE
    );

    @Getter @Setter
    private static MobGriefControl instance;

    @Getter @Setter
    private static GriefConfig griefConfig;

    private GriefListener listener;

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

        if (! SUPPORTED.contains(ModuleUtils.getPlatformType())) {
            logWarning("Mob grief control works on Spigot/Paper, Forge and NeoForge servers; "
                    + ModuleUtils.getPlatformType() + " does not report mob griefing, so nothing will be blocked here.");
            return;
        }

        listener = new GriefListener();
        ModuleUtils.listen(listener, this);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            BaseEventHandler.unbake(listener);
            listener = null;
        }
    }
}
