package host.plas.collections.timers;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionManager;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.ModuleRunnable;
import singularity.utils.UserUtils;

/**
 * Reminds online players who have levels waiting to be claimed.
 */
public class RemindTimer extends ModuleRunnable {
    public RemindTimer(int minutes) {
        super(StreamlineCollections.getInstance(), minutes * 60L * 20L, minutes * 60L * 20L);
    }

    @Override
    public void run() {
        for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) {
            CollectionManager.remind(player);
        }
    }
}
