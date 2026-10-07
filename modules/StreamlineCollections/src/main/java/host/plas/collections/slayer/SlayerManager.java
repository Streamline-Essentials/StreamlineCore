package host.plas.collections.slayer;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.Boards;
import host.plas.collections.data.LevelReward;
import host.plas.collections.data.Track;
import host.plas.collections.data.TrackedPlayer;
import host.plas.collections.database.TrackKeeper;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IGameplayHandler;
import singularity.modules.ModuleUtils;
import singularity.objects.ClickableMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongUnaryOperator;

/**
 * Slayer: how many of each mob a player has killed, with ten levels per mob whose rewards are
 * claimed from the menu.
 *
 * <p>Kills of players loaded on this server live in memory and are written by the module's save
 * timer and on quit. With {@code sync-stats} on, every count is also raised to the player's vanilla
 * {@code minecraft:killed} statistic, on join and at each statistics refresh; a sync never lowers
 * a count and never announces levels (reminders cover them).</p>
 */
public final class SlayerManager {
    private SlayerManager() {
    }

    @Getter @Setter
    private static SlayerConfig config;

    public enum ClaimResult { CLAIMED, DISABLED, ELSEWHERE, LOCKED, ALREADY_CLAIMED }

    /** Rebuilds the catalog from the configured thresholds. */
    public static void loadCatalog() {
        SlayerCatalog.build(config.getTiers(), config.getBossTiers());
    }

    public static boolean isEnabled() {
        return config.isEnabled();
    }

    public static boolean rewardsEnabled() {
        return config.isRewardsEnabled();
    }

    public static Optional<TrackedPlayer> getLoaded(String uuid) {
        return Track.SLAYER.loaded(uuid);
    }

    public static CompletableFuture<TrackedPlayer> snapshot(String uuid) {
        return Track.SLAYER.snapshot(uuid);
    }

    /** Whether {@code viewer} is looking at their own, live kills and may claim from them. */
    public static boolean canClaim(CosmicPlayer viewer, TrackedPlayer progress) {
        return StreamlineCollections.isTrackingServer()
                && viewer.getUuid().equals(progress.getIdentifier())
                && Track.SLAYER.isLive(progress);
    }

    // ---- tracking ------------------------------------------------------------------------------

    /** Adds kills to a loaded player's track and announces every level the count just completed. */
    public static void add(CosmicPlayer player, SlayerDefinition definition, long delta) {
        TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || delta <= 0) return;

        long before = progress.amount(definition.getId());
        long after = progress.add(definition.getId(), delta);
        for (int level = definition.tier(before) + 1; level <= definition.tier(after); level++) announceLevel(player, definition, level);
    }

    private static void announceLevel(CosmicPlayer player, SlayerDefinition definition, int level) {
        if (! rewardsEnabled()) {
            ModuleUtils.sendMessage(player, message("level-reached")
                    .replace("%level%", String.valueOf(level)).replace("%mob%", definition.getDisplayName()));
            return;
        }

        String reward = config.getReward(level).getDescription();
        new ClickableMessage()
                .text(message("level-complete").replace("%level%", String.valueOf(level)).replace("%mob%", definition.getDisplayName()))
                .text(message("level-complete-button"))
                .hover(message("level-complete-hover").replace("%reward%", reward))
                .run("/slayer open " + definition.getId())
                .send(player);
    }

    // ---- vanilla statistics --------------------------------------------------------------------

    /**
     * Raises a loaded player's counts to their vanilla {@code minecraft:killed} statistic. Waits
     * for the server thread, so call it off that thread.
     *
     * @return how many mobs' counts went up, or {@code -1} when the player could not be synced
     */
    public static int sync(CosmicPlayer player) {
        IGameplayHandler gameplay = Singularity.gameplay().orElse(null);
        TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (gameplay == null || progress == null || ! progress.isFullyLoaded()) return -1;

        Map<String, Long> killed = gameplay.statistics(player.getUuid(), "minecraft:killed", SlayerCatalog.entityIds());
        int raised = 0;
        for (SlayerDefinition definition : SlayerCatalog.all()) {
            long value = killed.getOrDefault(definition.getEntityType(), 0L);
            if (value > progress.amount(definition.getId())) {
                progress.set(definition.getId(), value);
                raised++;
            }
        }
        if (raised > 0) Boards.clear(Track.SLAYER);
        return raised;
    }

    // ---- admin edits ---------------------------------------------------------------------------

    /**
     * Changes a player's kills for the given mobs: live when they are loaded here, otherwise in
     * the database. Counts never go below zero. Levels reached this way are not announced.
     */
    public static CompletableFuture<Void> modify(String uuid, Collection<SlayerDefinition> mobs, LongUnaryOperator change) {
        Boards.clear(Track.SLAYER);
        Optional<TrackedPlayer> live = getLoaded(uuid);
        if (live.isPresent()) {
            for (SlayerDefinition mob : mobs) {
                live.get().set(mob.getId(), Math.max(0L, change.applyAsLong(live.get().amount(mob.getId()))));
            }
            live.get().save(true);
            return CompletableFuture.completedFuture(null);
        }
        return Track.SLAYER.snapshot(uuid).thenAccept(stored -> {
            for (SlayerDefinition mob : mobs) {
                stored.set(mob.getId(), Math.max(0L, change.applyAsLong(stored.amount(mob.getId()))));
            }
            Track.SLAYER.getKeeper().save(stored, false);
        });
    }

    /** Clears a player's kills and claimed levels for one mob, or for every mob when {@code mob} is null. */
    public static CompletableFuture<Void> reset(String uuid, SlayerDefinition mob) {
        Boards.clear(Track.SLAYER);
        Optional<TrackedPlayer> live = getLoaded(uuid);
        if (live.isPresent()) {
            if (mob == null) live.get().wipe();
            else live.get().reset(mob.getId());
            live.get().save(true);
            return CompletableFuture.completedFuture(null);
        }
        return Track.SLAYER.snapshot(uuid).thenAccept(stored -> {
            if (mob == null) stored.wipe();
            else stored.reset(mob.getId());
            Track.SLAYER.getKeeper().save(stored, false);
        });
    }

    // ---- rewards -------------------------------------------------------------------------------

    public static int unclaimed(TrackedPlayer progress, SlayerDefinition definition) {
        if (! rewardsEnabled()) return 0;
        int count = 0;
        for (int level = 1; level <= definition.tier(progress.amount(definition.getId())); level++) {
            if (! progress.isClaimed(definition.getId(), level)) count++;
        }
        return count;
    }

    public static int unclaimed(TrackedPlayer progress) {
        int count = 0;
        for (SlayerDefinition definition : SlayerCatalog.all()) count += unclaimed(progress, definition);
        return count;
    }

    /** Tells a player with levels waiting how many there are, with a link to the menu. */
    public static void remind(CosmicPlayer player) {
        if (! isEnabled() || ! rewardsEnabled()) return;
        TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || ! progress.isFullyLoaded()) return;

        int waiting = unclaimed(progress);
        if (waiting <= 0) return;

        String first = null;
        for (SlayerDefinition definition : SlayerCatalog.all()) {
            if (unclaimed(progress, definition) > 0) {
                first = definition.getId();
                break;
            }
        }

        new ClickableMessage()
                .text(message("remind").replace("%count%", String.valueOf(waiting)))
                .text(message("remind-button"))
                .hover(message("remind-hover"))
                .run(first == null ? "/slayer" : "/slayer open " + first)
                .send(player);
    }

    /** Claims a completed level for a loaded player and runs its reward. */
    public static ClaimResult claim(CosmicPlayer player, SlayerDefinition definition, int level) {
        if (! rewardsEnabled()) return ClaimResult.DISABLED;
        if (! StreamlineCollections.isTrackingServer()) return ClaimResult.ELSEWHERE;

        TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || level < 1 || level > definition.levels()
                || progress.amount(definition.getId()) < definition.threshold(level)) {
            return ClaimResult.LOCKED;
        }
        // The in-memory set is the guard: a second click finds the key already present.
        if (! progress.claim(definition.getId(), level)) return ClaimResult.ALREADY_CLAIMED;

        LevelReward reward = config.getReward(level);
        reward.run(player, "%level%", String.valueOf(level), "%mob%", definition.getDisplayName(), "%mob_id%", definition.getId());
        progress.save(true);

        ModuleUtils.sendMessage(player, message("claimed")
                .replace("%level%", String.valueOf(level))
                .replace("%mob%", definition.getDisplayName())
                .replace("%reward%", reward.getDescription()));
        return ClaimResult.CLAIMED;
    }

    // ---- leaderboards --------------------------------------------------------------------------

    public static CompletableFuture<List<TrackKeeper.Ranked>> top(SlayerDefinition definition) {
        List<String> ids = new ArrayList<>();
        ids.add(definition.getId());
        return Boards.top(Track.SLAYER, "id:" + definition.getId(), ids);
    }

    public static CompletableFuture<List<TrackKeeper.Ranked>> top(SlayerCategory category) {
        List<String> ids = new ArrayList<>();
        for (SlayerDefinition definition : SlayerCatalog.in(category)) ids.add(definition.getId());
        return Boards.top(Track.SLAYER, "category:" + category.id(), ids);
    }

    public static CompletableFuture<List<TrackKeeper.Ranked>> topOverall() {
        return Boards.top(Track.SLAYER, "overall", SlayerCatalog.ids());
    }

    public static String message(String key) {
        return config.getMessage(key);
    }

    /** Kill, level and completion counts across a set of mobs. */
    @Getter
    public static final class Totals {
        private int count;
        private int maxed;
        private int tiers;
        private int levels;
        private long kills;

        public static Totals of(TrackedPlayer progress, Iterable<SlayerDefinition> definitions) {
            Totals totals = new Totals();
            for (SlayerDefinition definition : definitions) {
                long amount = progress.amount(definition.getId());
                totals.count++;
                totals.kills += amount;
                totals.tiers += definition.tier(amount);
                totals.levels += definition.levels();
                if (definition.maxed(amount)) totals.maxed++;
            }
            return totals;
        }
    }
}
