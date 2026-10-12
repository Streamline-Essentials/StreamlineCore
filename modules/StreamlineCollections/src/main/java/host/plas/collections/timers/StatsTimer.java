package host.plas.collections.timers;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.StatFeeds;
import host.plas.collections.data.StatReader;
import host.plas.collections.progression.ProgressionManager;
import host.plas.collections.slayer.SlayerManager;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

/**
 * Reads online players' vanilla statistics: what statistic-fed collections and progression
 * categories gained, and slayer's kill floors. Each player's reads run on a {@link StatReader}
 * thread, since every read waits for the server thread.
 */
public class StatsTimer extends ModuleRunnable {
    public StatsTimer(int seconds) {
        super(StreamlineCollections.getInstance(), seconds * 20L, seconds * 20L);
    }

    @Override
    public void run() {
        for (CosmicPlayer online : UserUtils.getOnlinePlayers().values()) {
            StatReader.submit("read statistics for " + online.getCurrentName(), () -> {
                if (StreamlineCollections.getMainConfig().isTrackStatistics()) StatFeeds.refresh(online);
                ProgressionManager.refresh(online);
                if (SlayerManager.isEnabled() && SlayerManager.getConfig().isSyncStats()) SlayerManager.sync(online);
            });
        }
    }
}
