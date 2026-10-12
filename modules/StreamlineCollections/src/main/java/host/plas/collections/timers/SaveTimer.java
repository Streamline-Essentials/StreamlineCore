package host.plas.collections.timers;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.Track;
import host.plas.collections.data.TrackedPlayer;
import singularity.scheduler.ModuleRunnable;

/**
 * Writes changed collections, slayer and progression to the database, and flushes the placed-block
 * store.
 */
public class SaveTimer extends ModuleRunnable {
    public SaveTimer(int seconds) {
        super(StreamlineCollections.getInstance(), seconds * 20L, seconds * 20L);
    }

    @Override
    public void run() {
        for (Track track : Track.values()) {
            for (TrackedPlayer player : track.getLoader().getLoaded()) {
                if (player.isDirty()) player.save(true);
            }
        }
        if (StreamlineCollections.getPlacedBlocks() != null) StreamlineCollections.getPlacedBlocks().flush(false);
    }
}
