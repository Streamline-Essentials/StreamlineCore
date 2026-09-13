package host.plas.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicResourcePack;
import host.plas.ResourcePackUtils;
import host.plas.runnables.PackTicker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainListener implements BaseEventListener {
    @Getter @Setter
    private static ConcurrentHashMap<CosmicPlayer, Boolean> packedMap = new ConcurrentHashMap<>();

    @BaseProcessor
    public void onJoin(LoginCompletedEvent event) {
        CosmicSender sender = event.getSender();
        if (! (sender instanceof CosmicPlayer)) return;
        String name = sender.getCurrentName();
        if (name == null) return;
        AtomicBoolean exempt = new AtomicBoolean(false);
        ResourcePackUtils.getConfigs().getExemptStartsWith().forEach(s -> {
            if (exempt.get()) return;

            if (name.startsWith(s)) {
                exempt.set(true);
            }
        });
        if (exempt.get()) return;

        CosmicPlayer player = (CosmicPlayer) sender;
        ISingularityExtension.PlatformType type = ModuleUtils.getPlatformType();
        CosmicResourcePack pack = ResourcePackUtils.getConfigs().getResourcePack();
        new PackTicker(player, type, pack);
    }

    @BaseProcessor
    public void onLeave(LogoutEvent event) {
        CosmicSender sender = event.getSender();
        if (! (sender instanceof CosmicPlayer)) return;
        CosmicPlayer player = (CosmicPlayer) sender;

        getPackedMap().remove(player);
    }
}
