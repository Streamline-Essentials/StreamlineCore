package host.plas;

import host.plas.runnables.PackTicker;
import lombok.Getter;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;
import host.plas.commands.PackCommand;
import host.plas.configs.Configs;
import host.plas.listeners.MainListener;
import org.pf4j.PluginWrapper;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class ResourcePackUtils extends SimpleModule {
    @Getter
    private static ResourcePackUtils instance;
    @Getter
    private static Configs configs;
    @Getter
    private static MainListener mainListener;

    public ResourcePackUtils(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ConcurrentSkipListSet<String> authors() {
        return new ConcurrentSkipListSet<>(List.of("Quaint"));
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        configs = new Configs();
        mainListener = new MainListener();
        ModuleUtils.listen(mainListener, this);

        new PackCommand().register();
    }

    @Override
    public void onDisable() {
        PackTicker.clearAll();
    }
}
