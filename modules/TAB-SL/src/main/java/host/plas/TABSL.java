package host.plas;

import host.plas.configs.TabConfig;
import host.plas.managers.TABManager;
import host.plas.ratapi.TABExpansion;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.SimpleModule;
import org.pf4j.PluginWrapper;

import java.util.List;

public class TABSL extends SimpleModule {
    @Getter @Setter
    private static TABSL instance;

    @Getter @Setter
    private static TABExpansion tabExpansion;

    @Getter @Setter
    private static TabConfig tabConfig;

    public TABSL(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(List.of(
        ));
    }

    @Override
    public void onEnable() {
        instance = this;

        tabConfig = new TabConfig();

        tabExpansion = new TABExpansion();

        TABManager.init();
    }
}
