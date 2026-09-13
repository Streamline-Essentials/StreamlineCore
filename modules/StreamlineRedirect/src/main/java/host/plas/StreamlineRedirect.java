package host.plas;

import lombok.Getter;
import lombok.Setter;
import singularity.modules.SimpleModule;
import host.plas.configs.MainConfig;
import host.plas.events.MainListener;
import org.pf4j.PluginWrapper;

public class StreamlineRedirect extends SimpleModule {
    @Getter @Setter
    private static StreamlineRedirect instance;
    @Getter @Setter
    private static MainConfig mainConfig;
    @Getter @Setter
    private static MainListener mainListener;

    public StreamlineRedirect(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;

        mainConfig = new MainConfig();

        mainListener = new MainListener();
    }
}
