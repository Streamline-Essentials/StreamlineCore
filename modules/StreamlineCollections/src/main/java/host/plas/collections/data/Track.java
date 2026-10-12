package host.plas.collections.data;

import host.plas.collections.database.TrackKeeper;
import host.plas.collections.database.TrackLoader;
import lombok.Getter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * One kind of per-player progress this module keeps: an amount per id, plus claimed levels where
 * the track has levels to claim. Each track has its own tables, keeper and loaded players, and
 * shares the {@link TrackedPlayer} shape, so saving, loading and leaderboards are written once.
 */
@Getter
public enum Track {
    /** Amount collected per collection id; claims per collection level. */
    COLLECTIONS("collections", "collection_claims"),
    /** Kills per mob id; claims per slayer level. */
    SLAYER("slayer", "slayer_claims"),
    /** Amount per progression category key, skill XP and the last announced star; no claims. */
    PROGRESSION("progression", null),
    ;

    /** The amounts table, without the core's table prefix. */
    private final String amountsTable;
    /** The claims table, without the core's table prefix, or {@code null} when the track has no claims. */
    private final String claimsTable;

    private TrackKeeper keeper;
    private TrackLoader loader;

    Track(String amountsTable, String claimsTable) {
        this.amountsTable = amountsTable;
        this.claimsTable = claimsTable;
    }

    /** The live progress of a player loaded on this server. */
    public Optional<TrackedPlayer> loaded(String uuid) {
        return loader.get(uuid);
    }

    /**
     * A player's progress for viewing: the live object when they are loaded here, otherwise a
     * fresh read from the database that is not cached, so it never overwrites anyone's progress.
     */
    public CompletableFuture<TrackedPlayer> snapshot(String uuid) {
        Optional<TrackedPlayer> live = loaded(uuid);
        if (live.isPresent()) return CompletableFuture.completedFuture(live.get());
        return keeper.load(uuid).thenApply(stored -> stored.orElseGet(() -> new TrackedPlayer(this, uuid)));
    }

    /** Whether {@code progress} is the live copy of a player loaded on this server. */
    public boolean isLive(TrackedPlayer progress) {
        return loaded(progress.getIdentifier()).orElse(null) == progress;
    }

    /** Creates every track's keeper and loader; called when the module enables. */
    public static void initAll() {
        for (Track track : values()) {
            track.keeper = new TrackKeeper(track);
            track.loader = new TrackLoader(track);
        }
    }
}
