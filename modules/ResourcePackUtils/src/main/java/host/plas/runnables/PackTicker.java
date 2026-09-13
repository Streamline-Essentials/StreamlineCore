package host.plas.runnables;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicResourcePack;
import singularity.scheduler.ModuleRunnable;
import host.plas.ResourcePackUtils;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

@Setter @Getter
public class PackTicker extends ModuleRunnable {
    @Getter @Setter
    private static ConcurrentSkipListMap<Integer, PackTicker> tickers = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static AtomicInteger tickerCounter = new AtomicInteger(0);

    public static int getNextTickerIndex() {
        return getTickerCounter().getAndIncrement();
    }

    public static void append(PackTicker ticker) {
        getTickers().put(getNextTickerIndex(), ticker);
    }

    public static void clearAll() {
        getTickers().forEach((i, t) -> t.cancel());

        getTickers().clear();
        getTickerCounter().set(0);
    }

    private CosmicPlayer player;
    private ISingularityExtension.PlatformType type;
    private CosmicResourcePack pack;

    public PackTicker(CosmicPlayer player, ISingularityExtension.PlatformType type, CosmicResourcePack pack) {
        super(ResourcePackUtils.getInstance(), 1, ResourcePackUtils.getConfigs().connectWait());
        this.player = player;
        this.type = type;
        this.pack = pack;
    }

    @Override
    public void run() {
        if (player == null || pack == null || type == null || ! player.isOnline()) {
            cancel();
            return;
        }

        try {
            ResourcePackUtils.getInstance().logInfo("&fSending resource pack to '" + player.getDisplayName() + "&f'...");

            switch (type) {
                case BUNGEE:
                case VELOCITY:
                    if (ResourcePackUtils.getConfigs().isNetworkHandled()) {
                        ModuleUtils.sendResourcePack(pack, player);
                    } else {
                        // do nothing;
                    }
                    break;
                case SPIGOT:
                    if (ResourcePackUtils.getConfigs().isNetworkHandled()) {
                        // do nothing
                    } else {
                        ModuleUtils.sendResourcePack(pack, player);
                    }
                    break;
            }
        } catch (Exception e) {
            ResourcePackUtils.getInstance().logSevere("&cFailed to send resource pack to '" + player.getCurrentName() + "&c'!");
            e.printStackTrace();
        }

        cancel();
    }
}
