package host.plas.stonedamager;

import host.plas.stonedamager.commands.ReloadCommand;
import host.plas.stonedamager.config.DamagerConfig;
import host.plas.stonedamager.tasks.DamageTicker;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;

import java.util.ArrayList;
import java.util.List;

public class StoneDamager extends SimpleModule {
    @Getter @Setter
    private static StoneDamager instance;

    @Getter @Setter
    private static DamagerConfig damagerConfig;

    private DamageTicker ticker;

    public StoneDamager(PluginWrapper wrapper) {
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
        instance = this;

        damagerConfig = new DamagerConfig();
        logInfo("&fLoaded &a" + damagerConfig.getSelections().size() + " &fdamager selections.");

        if (Singularity.gameplay().isEmpty()) {
            logWarning("Stone damagers need a server with worlds; " + ModuleUtils.getPlatformType()
                    + " has none, so nothing will be damaged here.");
            return;
        }

        ticker = new DamageTicker();
    }

    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }
}
