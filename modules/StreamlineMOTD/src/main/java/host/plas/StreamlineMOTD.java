package host.plas;

import host.plas.config.MOTDConfig;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleUtils;
import singularity.modules.CosmicModule;
import host.plas.events.MainListener;
import host.plas.ratapi.MOTDExpansion;
import host.plas.timers.MOTDRunner;
import org.pf4j.PluginWrapper;

public class StreamlineMOTD extends CosmicModule {
    @Getter @Setter
    private static StreamlineMOTD instance;

    @Getter @Setter
    private static MOTDConfig config;

    @Getter @Setter
    private static MOTDRunner runner;

    @Getter @Setter
    private static MainListener listener;

    @Getter @Setter
    private static MOTDExpansion expansion;

    @Override
    protected void registerCommands() {

    }

    public StreamlineMOTD(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;

        config = new MOTDConfig();
        config.updateResponse();
        runner = new MOTDRunner();
        listener = new MainListener();

        ModuleUtils.listen(listener, this);

        expansion = new MOTDExpansion();
    }

    @Override
    public void onDisable() {
        if (runner != null) {
            runner.cancel();
            runner = null;
        }
    }
}
