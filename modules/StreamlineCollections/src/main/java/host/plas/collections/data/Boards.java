package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.database.TrackKeeper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Leaderboards for every {@link Track}, each cached for a minute. Progress loaded on this server is
 * written before a board is read, so it is counted.
 */
public final class Boards {
    private Boards() {
    }

    public static final int BOARD_SIZE = 100;
    private static final long CACHE_MILLIS = 60_000L;

    private static final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private static final class Cached {
        private final long at;
        private final List<TrackKeeper.Ranked> rows;

        private Cached(long at, List<TrackKeeper.Ranked> rows) {
            this.at = at;
            this.rows = rows;
        }
    }

    /** The top players by the summed amounts of {@code ids}. */
    public static CompletableFuture<List<TrackKeeper.Ranked>> top(Track track, String key, List<String> ids) {
        return cached(track, key, () -> track.getKeeper().top(ids, BOARD_SIZE));
    }

    /** The top players by each id's amount times its weight, summed. */
    public static CompletableFuture<List<TrackKeeper.Ranked>> weighted(Track track, String key, Map<String, Double> weights) {
        return cached(track, key, () -> track.getKeeper().topWeighted(weights, BOARD_SIZE));
    }

    private static CompletableFuture<List<TrackKeeper.Ranked>> cached(Track track, String key, Supplier<List<TrackKeeper.Ranked>> read) {
        String cacheKey = track.name() + ":" + key;
        Cached hit = cache.get(cacheKey);
        if (hit != null && System.currentTimeMillis() - hit.at < CACHE_MILLIS) {
            return CompletableFuture.completedFuture(hit.rows);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (StreamlineCollections.isTrackingServer()) {
                for (TrackedPlayer player : track.getLoader().getLoaded()) {
                    if (player.isDirty() && player.isFullyLoaded()) player.save(false);
                }
            }
            List<TrackKeeper.Ranked> rows = read.get();
            cache.put(cacheKey, new Cached(System.currentTimeMillis(), rows));
            return rows;
        });
    }

    /** Drops one track's cached boards. */
    public static void clear(Track track) {
        cache.keySet().removeIf(key -> key.startsWith(track.name() + ":"));
    }

    public static void clearAll() {
        cache.clear();
    }

    /** The 1-based rank of {@code uuid} in {@code rows}, or {@code -1} when absent. */
    public static int rankOf(List<TrackKeeper.Ranked> rows, String uuid) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).uuid.equals(uuid)) return i + 1;
        }
        return -1;
    }
}
