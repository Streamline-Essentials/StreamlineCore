package host.plas.collections.timers;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionPlayer;
import host.plas.collections.data.StatFeeds;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

/**
 * Counts what statistic-fed collections gained, writes changed progress to the database, and
 * flushes the placed-block store.
 */
public class SaveTimer extends ModuleRunnable {
    public SaveTimer(int seconds) {
        super(StreamlineCollections.getInstance(), seconds * 20L, seconds * 20L);
    }

    @Override
    public void run() {
        if (StreamlineCollections.getMainConfig().isTrackStatistics()) {
            for (CosmicPlayer online : UserUtils.getOnlinePlayers().values()) {
                try {
                    StatFeeds.refresh(online);
                } catch (Throwable t) {
                    StreamlineCollections.getInstance().logWarning("Could not count statistics for " + online.getCurrentName() + ": " + t.getMessage());
                }
            }
        }
        for (CollectionPlayer player : StreamlineCollections.getLoader().getLoaded()) {
            if (player.isDirty()) player.save(true);
        }
        if (StreamlineCollections.getPlacedBlocks() != null) StreamlineCollections.getPlacedBlocks().flush(false);
    }
}
