package host.plas.collections.database;

import host.plas.collections.data.Track;
import host.plas.collections.data.TrackedPlayer;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

/**
 * The players loaded on this server for one {@link Track}.
 */
public class TrackLoader extends Loader<TrackedPlayer> {
    private final Track track;

    public TrackLoader(Track track) {
        this.track = track;
    }

    @Override
    public DBKeeper<TrackedPlayer> getKeeper() {
        return track.getKeeper();
    }

    @Override
    public TrackedPlayer getConsole() {
        return null;
    }

    @Override
    public void fireLoadEvents(TrackedPlayer player) {
    }

    @Override
    public TrackedPlayer instantiate(String identifier) {
        return new TrackedPlayer(track, identifier);
    }

    @Override
    public void fireCreateEvents(TrackedPlayer player) {
    }
}
