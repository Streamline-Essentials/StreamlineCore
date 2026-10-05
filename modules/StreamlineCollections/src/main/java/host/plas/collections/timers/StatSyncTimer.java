package host.plas.collections.timers;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.StatSync;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

/**
 * Syncs online players' collections from their vanilla statistics, catching up whatever the
 * platform's events do not report.
 */
public class StatSyncTimer extends ModuleRunnable {
    public StatSyncTimer(int minutes) {
        super(StreamlineCollections.getInstance(), minutes * 60L * 20L, minutes * 60L * 20L);
    }

    @Override
    public void run() {
        for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) {
            try {
                StatSync.sync(player, true);
            } catch (Throwable t) {
                StreamlineCollections.getInstance().logWarning("Stat sync failed for " + player.getCurrentName() + ": " + t.getMessage());
            }
        }
    }
}
