package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;
import lombok.Getter;
import lombok.Setter;
import singularity.loading.Loadable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One player's progress on one {@link Track}: an amount per id (collection, mob or progression
 * key) and the levels claimed. Claims are keyed {@code <id>:<level>}.
 */
@Getter @Setter
public class TrackedPlayer implements Loadable<TrackedPlayer> {
    private final Track track;
    private String identifier;
    private String name;
    private Map<String, Long> amounts;
    private Set<String> claimed;
    /** Set when progress changed since the last save. */
    private volatile boolean dirty;
    /**
     * For each statistic-fed collection, the amount its statistic stood for when last counted
     * on this server. Kept in memory only; see {@link StatFeeds}.
     */
    private final transient Map<String, Long> baselines = new ConcurrentHashMap<>();

    private boolean fullyLoaded = false;

    /**
     * Set while {@link #augment} waits on the stored record. A save in that window would replace
     * the stored progress with this instance's partial state, so it is held until the record has
     * been merged in.
     */
    private volatile boolean loadInFlight = false;
    private volatile boolean savePendingAfterLoad = false;

    public TrackedPlayer(Track track, String identifier) {
        this.track = track;
        this.identifier = identifier;
        this.name = "";
        this.amounts = new ConcurrentHashMap<>();
        this.claimed = ConcurrentHashMap.newKeySet();
        this.dirty = false;
    }

    public static String claimKey(String collection, int level) {
        return collection + ":" + level;
    }

    public long amount(String collection) {
        Long amount = amounts.get(collection);
        return amount == null ? 0L : amount;
    }

    /** Adds {@code delta} and returns the new amount. */
    public long add(String collection, long delta) {
        dirty = true;
        return amounts.merge(collection, delta, Long::sum);
    }

    public void set(String collection, long amount) {
        dirty = true;
        if (amount <= 0) amounts.remove(collection);
        else amounts.put(collection, amount);
    }

    public boolean isClaimed(String collection, int level) {
        return claimed.contains(claimKey(collection, level));
    }

    /** Marks a level claimed, returning {@code false} if it already was. */
    public boolean claim(String collection, int level) {
        boolean added = claimed.add(claimKey(collection, level));
        if (added) dirty = true;
        return added;
    }

    /** Clears one id's amount and claimed levels. */
    public void reset(String id) {
        amounts.remove(id);
        claimed.removeIf(key -> key.startsWith(id + ":"));
        dirty = true;
    }

    public void wipe() {
        amounts.clear();
        claimed.clear();
        dirty = true;
    }

    @Override
    public void save(boolean async) {
        if (loadInFlight) {
            savePendingAfterLoad = true;
            return;
        }

        dirty = false;
        track.getKeeper().save(this, async);
    }

    @Override
    public void unload() {
        track.getLoader().unload(this);
    }

    @Override
    public void load() {
        track.getLoader().load(this);
    }

    @Override
    public boolean isLoaded() {
        return track.getLoader().isLoaded(getIdentifier());
    }

    @Override
    public void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    @Override
    public TrackedPlayer augment(CompletableFuture<Optional<TrackedPlayer>> augmentation, boolean isGet) {
        this.fullyLoaded = false;
        this.loadInFlight = true;

        augmentation.whenComplete((optional, throwable) -> {
            boolean saveNow = savePendingAfterLoad;

            if (throwable != null) {
                StreamlineCollections.getInstance().logWarning("Failed to load " + track.getAmountsTable() + " for " + identifier + ": " + throwable.getMessage());
            } else if (optional.isPresent()) {
                TrackedPlayer stored = optional.get();

                // Progress made before the load finished is kept on top of the stored progress.
                stored.amounts.forEach((collection, amount) -> this.amounts.merge(collection, amount, Long::sum));
                this.claimed.addAll(stored.claimed);
                if ((this.name == null || this.name.isEmpty()) && stored.name != null) this.name = stored.name;
            } else if (! isGet) {
                saveNow = true;
            }

            this.loadInFlight = false;
            this.savePendingAfterLoad = false;
            this.fullyLoaded = true;

            if (saveNow) save();
        });

        return this;
    }
}
