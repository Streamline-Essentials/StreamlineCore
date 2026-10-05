package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.database.CollectionKeeper;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.objects.ClickableMessage;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collection progress, claims, reminders and leaderboards.
 */
public final class CollectionManager {
    private CollectionManager() {
    }

    public static final int BOARD_SIZE = 100;
    private static final long BOARD_CACHE_MILLIS = 60_000L;

    @Getter @Setter
    private static Catalog catalog = new Catalog();

    private static final Map<String, CachedBoard> boards = new ConcurrentHashMap<>();

    private static final class CachedBoard {
        private final long at;
        private final List<CollectionKeeper.Ranked> rows;

        private CachedBoard(long at, List<CollectionKeeper.Ranked> rows) {
            this.at = at;
            this.rows = rows;
        }
    }

    public enum ClaimResult { CLAIMED, DISABLED, ELSEWHERE, LOCKED, ALREADY_CLAIMED }

    /** The live progress of a player loaded on this server. */
    public static Optional<CollectionPlayer> getLoaded(String uuid) {
        return StreamlineCollections.getLoader().get(uuid);
    }

    /**
     * A player's progress for viewing: the live object when they are loaded here, otherwise a
     * fresh read from the database that is not cached, so it never overwrites anyone's progress.
     */
    public static CompletableFuture<CollectionPlayer> snapshot(String uuid) {
        Optional<CollectionPlayer> loaded = getLoaded(uuid);
        if (loaded.isPresent()) return CompletableFuture.completedFuture(loaded.get());
        return StreamlineCollections.getKeeper().load(uuid)
                .thenApply(stored -> stored.orElseGet(() -> new CollectionPlayer(uuid)));
    }

    /** Whether {@code viewer} is looking at their own, live progress and may claim from it. */
    public static boolean canClaim(CosmicPlayer viewer, CollectionPlayer progress) {
        return StreamlineCollections.isTrackingServer()
                && viewer.getUuid().equals(progress.getIdentifier())
                && getLoaded(progress.getIdentifier()).orElse(null) == progress;
    }

    /** Adds to a loaded player's collection, announcing each level it completes. */
    public static void add(CosmicPlayer player, String collection, long delta) {
        add(player, collection, delta, true);
    }

    /**
     * Adds to a loaded player's collection, returning how many levels that completed; each is
     * announced only when {@code announce} is set.
     */
    public static int add(CosmicPlayer player, String collection, long delta, boolean announce) {
        if (delta <= 0) return 0;
        CollectionDefinition definition = catalog.get(collection).orElse(null);
        CollectionPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (definition == null || progress == null) return 0;

        long before = progress.amount(definition.getId());
        long after = progress.add(definition.getId(), delta);

        int from = definition.tier(before) + 1;
        int to = definition.tier(after);
        if (announce) {
            for (int level = from; level <= to; level++) announceLevel(player, definition, level);
        }
        return Math.max(0, to - from + 1);
    }

    private static void announceLevel(CosmicPlayer player, CollectionDefinition definition, int level) {
        boolean rewards = StreamlineCollections.getMainConfig().isRewardsEnabled();
        String collectionName = definition.getDisplayName();

        if (! rewards) {
            ModuleUtils.sendMessage(player, message("level-reached")
                    .replace("%level%", String.valueOf(level)).replace("%collection%", collectionName));
            return;
        }

        String reward = StreamlineCollections.getMainConfig().getReward(level).getDescription();
        new ClickableMessage()
                .text(message("level-complete").replace("%level%", String.valueOf(level)).replace("%collection%", collectionName))
                .text(message("level-complete-button"))
                .hover(message("level-complete-hover").replace("%reward%", reward))
                .run("/collections open " + definition.getId())
                .send(player);
    }

    /** The levels a player completed but has not claimed. */
    public static int unclaimed(CollectionPlayer progress) {
        if (! StreamlineCollections.getMainConfig().isRewardsEnabled()) return 0;
        int count = 0;
        for (CollectionDefinition definition : catalog.all()) count += unclaimed(progress, definition);
        return count;
    }

    public static int unclaimed(CollectionPlayer progress, CollectionDefinition definition) {
        int count = 0;
        int tier = definition.tier(progress.amount(definition.getId()));
        for (int level = 1; level <= tier; level++) {
            if (! progress.isClaimed(definition.getId(), level)) count++;
        }
        return count;
    }

    /** Claims a completed level for {@code player} and runs its reward. */
    public static ClaimResult claim(CosmicPlayer player, CollectionDefinition definition, int level) {
        if (! StreamlineCollections.getMainConfig().isRewardsEnabled()) return ClaimResult.DISABLED;
        if (! StreamlineCollections.isTrackingServer()) return ClaimResult.ELSEWHERE;

        CollectionPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || level < 1 || level > definition.levels()
                || progress.amount(definition.getId()) < definition.threshold(level)) {
            return ClaimResult.LOCKED;
        }
        if (! progress.claim(definition.getId(), level)) return ClaimResult.ALREADY_CLAIMED;

        LevelReward reward = StreamlineCollections.getMainConfig().getReward(level);
        for (String command : reward.getCommands()) {
            String resolved = command
                    .replace("%player%", player.getCurrentName())
                    .replace("%uuid%", player.getUuid())
                    .replace("%level%", String.valueOf(level))
                    .replace("%collection%", definition.getDisplayName())
                    .replace("%collection_id%", definition.getId());
            if (resolved.startsWith("/")) resolved = resolved.substring(1);
            Singularity.getInstance().getUserManager().runAs(UserUtils.getConsole(), true, resolved);
        }

        progress.save(true);

        ModuleUtils.sendMessage(player, message("claimed")
                .replace("%level%", String.valueOf(level))
                .replace("%collection%", definition.getDisplayName())
                .replace("%reward%", reward.getDescription()));
        return ClaimResult.CLAIMED;
    }

    /** Tells a player with levels waiting how many there are, with a link to the menu. */
    public static void remind(CosmicPlayer player) {
        if (! StreamlineCollections.getMainConfig().isRewardsEnabled()) return;
        CollectionPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || ! progress.isFullyLoaded()) return;

        int count = unclaimed(progress);
        if (count <= 0) return;

        String first = null;
        for (CollectionDefinition definition : catalog.all()) {
            if (unclaimed(progress, definition) > 0) {
                first = definition.getId();
                break;
            }
        }

        new ClickableMessage()
                .text(message("remind").replace("%count%", String.valueOf(count)))
                .text(message("remind-button"))
                .hover(message("remind-hover"))
                .run(first == null ? "/collections" : "/collections open " + first)
                .send(player);
    }

    /**
     * The top players for the given collections, summed, cached for a minute. Progress loaded on
     * this server is written first so it is counted.
     */
    public static CompletableFuture<List<CollectionKeeper.Ranked>> board(String cacheKey, List<String> collectionIds) {
        CachedBoard cached = boards.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.at < BOARD_CACHE_MILLIS) {
            return CompletableFuture.completedFuture(cached.rows);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (StreamlineCollections.isTrackingServer()) {
                for (CollectionPlayer player : StreamlineCollections.getLoader().getLoaded()) {
                    if (player.isDirty() && player.isFullyLoaded()) player.save(false);
                }
            }
            List<CollectionKeeper.Ranked> rows = StreamlineCollections.getKeeper().top(collectionIds, BOARD_SIZE);
            boards.put(cacheKey, new CachedBoard(System.currentTimeMillis(), rows));
            return rows;
        });
    }

    public static CompletableFuture<List<CollectionKeeper.Ranked>> boardFor(CollectionDefinition definition) {
        List<String> ids = new ArrayList<>();
        ids.add(definition.getId());
        return board("id:" + definition.getId(), ids);
    }

    public static CompletableFuture<List<CollectionKeeper.Ranked>> boardFor(CollectionCategory category) {
        List<String> ids = new ArrayList<>();
        for (CollectionDefinition definition : catalog.inCategory(category)) ids.add(definition.getId());
        return board("category:" + category.getId(), ids);
    }

    public static CompletableFuture<List<CollectionKeeper.Ranked>> overallBoard() {
        List<String> ids = new ArrayList<>();
        for (CollectionDefinition definition : catalog.all()) ids.add(definition.getId());
        return board("overall", ids);
    }

    public static void clearBoards() {
        boards.clear();
    }

    public static String message(String key) {
        return StreamlineCollections.getMainConfig().getMessage(key);
    }
}
