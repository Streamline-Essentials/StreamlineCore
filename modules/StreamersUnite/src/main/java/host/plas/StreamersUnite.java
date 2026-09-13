package host.plas;

import host.plas.commands.*;
import host.plas.config.MainConfig;
import host.plas.config.StreamerConfig;
import host.plas.data.LiveManager;
import host.plas.events.MainListener;
import host.plas.ratapi.StreamerExpansion;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.SimpleModule;
import org.pf4j.PluginWrapper;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public final class StreamersUnite extends SimpleModule {
    @Getter @Setter
    private static StreamersUnite instance;

    @Getter @Setter
    private static MainConfig mainConfig;
    @Getter @Setter
    private static StreamerConfig streamerConfig;

    @Getter @Setter
    private static MainListener mainListener;

    @Getter @Setter
    private static StreamerExpansion streamerExpansion;

    @Override
    public void registerCommands() {
        setCommands(new ArrayList<>(List.of(
                new BroadcastLiveCMD(),
                new CheckLiveCMD(),
                new GoLiveCMD(),
                new GoOfflineCMD(),
                new SetUpStreamerCMD()
        )));
    }

    public StreamersUnite(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        setInstance(this);

        setMainConfig(new MainConfig());
        setStreamerConfig(new StreamerConfig());

        setMainListener(new MainListener());

        setStreamerExpansion(new StreamerExpansion());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        LiveManager.getCurrentlyLive().clear();

        getStreamerExpansion().stop();
    }
}
