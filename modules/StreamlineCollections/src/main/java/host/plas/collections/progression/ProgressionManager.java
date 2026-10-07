package host.plas.collections.progression;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.Boards;
import host.plas.collections.data.Track;
import host.plas.collections.data.TrackedPlayer;
import host.plas.collections.database.TrackKeeper;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IGameplayHandler;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongUnaryOperator;

/**
 * Progression: cosmetic stars and skills, with no gameplay effect.
 *
 * <p>Every {@link ProgressionStat} is worth its amount times its weight in XP, and the total XP
 * gives the star level through {@link StarsFormula}; stars are never stored. Each player's amounts
 * live on the {@link Track#PROGRESSION} track: one row per category and skill, plus
 * {@code meta:last_star} (the highest star announced) and {@code adj:<category>} (staff changes to a
 * statistic-backed category, kept so a sync does not undo them).</p>
 *
 * <p>Statistic-backed categories gain what their statistic gained since it was last read on this
 * server, so a network's servers add into one amount; with {@code sync-stats} on they are also
 * floored at this server's statistic. Boards rank offline players from the stored rows, and weigh
 * them in the query, so a weight change applies to everyone at once.</p>
 */
public final class ProgressionManager {
    private ProgressionManager() {
    }

    @Getter @Setter
    private static ProgressionConfig config;

    public static final String LAST_STAR = "meta:last_star";
    private static final String ADJUST = "adj:";

    public static boolean isEnabled() {
        return config.isEnabled();
    }

    public static Optional<TrackedPlayer> getLoaded(String uuid) {
        return Track.PROGRESSION.loaded(uuid);
    }

    public static CompletableFuture<TrackedPlayer> snapshot(String uuid) {
        return Track.PROGRESSION.snapshot(uuid);
    }

    // ---- values --------------------------------------------------------------------------------

    public static double weight(ProgressionStat stat) {
        return config.weight(stat);
    }

    public static double totalXp(TrackedPlayer progress) {
        double total = 0;
        for (ProgressionStat stat : ProgressionStat.values()) total += progress.amount(stat.key()) * weight(stat);
        return total;
    }

    public static long stars(TrackedPlayer progress) {
        return StarsFormula.starsFromTotalXp(totalXp(progress));
    }

    public static long skillXp(TrackedPlayer progress, Skill skill) {
        return progress.amount(skill.key());
    }

    public static int skillLevel(TrackedPlayer progress, Skill skill) {
        double perLevel = config.getSkillXpPerLevel();
        return perLevel <= 0 ? 0 : (int) (skillXp(progress, skill) / perLevel);
    }

    /** Every category's weight, by key, for weighted boards. */
    public static Map<String, Double> weightsByKey() {
        Map<String, Double> weights = new LinkedHashMap<>();
        for (ProgressionStat stat : ProgressionStat.values()) weights.put(stat.key(), weight(stat));
        return weights;
    }

    // ---- counting ------------------------------------------------------------------------------

    /** Adds to a counted category of a loaded player. */
    public static void count(String uuid, ProgressionStat stat, long amount) {
        if (! isEnabled() || amount <= 0) return;
        getLoaded(uuid).ifPresent(progress -> progress.add(stat.key(), amount));
    }

    /** Adds skill XP to a loaded player. */
    public static void train(String uuid, Skill skill, long amount) {
        if (! isEnabled() || skill == null || amount <= 0) return;
        getLoaded(uuid).ifPresent(progress -> progress.add(skill.key(), amount));
    }

    // ---- vanilla statistics --------------------------------------------------------------------

    /**
     * Reads every statistic-backed category's current figure for a player online here, by
     * category; empty off game servers. Waits for the server thread, so call it off that thread.
     */
    public static Map<ProgressionStat, Long> readVanilla(String uuid) {
        Map<ProgressionStat, Long> figures = new EnumMap<>(ProgressionStat.class);
        IGameplayHandler gameplay = Singularity.gameplay().orElse(null);
        if (gameplay == null) return figures;

        Map<String, Set<String>> wanted = new HashMap<>();
        for (ProgressionStat stat : ProgressionStat.values()) {
            if (! stat.hasVanilla()) continue;
            Set<String> ids = wanted.computeIfAbsent(stat.getVanillaType(), k -> new HashSet<>());
            for (String id : stat.getVanillaIds()) ids.add(id);
        }
        Map<String, Map<String, Long>> values = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : wanted.entrySet()) {
            values.put(entry.getKey(), gameplay.statistics(uuid, entry.getKey(), entry.getValue()));
        }

        for (ProgressionStat stat : ProgressionStat.values()) {
            if (! stat.hasVanilla()) continue;
            Map<String, Long> ofType = values.getOrDefault(stat.getVanillaType(), new HashMap<>());
            long sum = 0;
            if (stat.sumsEveryId()) {
                for (long value : ofType.values()) sum += value;
            } else {
                for (String id : stat.getVanillaIds()) sum += ofType.getOrDefault(id, 0L);
            }
            figures.put(stat, sum / stat.getDivisor());
        }
        return figures;
    }

    /**
     * Brings a loaded player up to date with their statistics: statistic-read categories gain what
     * their statistic gained since the last read (a first read only sets the baseline), the skills
     * those feed train by the same gain, and with {@code sync-stats} on every statistic-backed
     * category is raised to its statistic plus any staff change. Waits for the server thread, so
     * call it off that thread.
     */
    public static void refresh(CosmicPlayer player) {
        if (! isEnabled() || ! StreamlineCollections.isTrackingServer()) return;
        TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || ! progress.isFullyLoaded()) return;

        Map<ProgressionStat, Long> figures = readVanilla(player.getUuid());
        for (Map.Entry<ProgressionStat, Long> entry : figures.entrySet()) {
            ProgressionStat stat = entry.getKey();
            long now = entry.getValue();
            Long before = progress.getBaselines().put(stat.key(), now);
            long gained = before == null ? 0L : Math.max(0L, now - before);

            if (! stat.isCounted() && gained > 0) progress.add(stat.key(), gained);
            trainFrom(progress, stat, gained);

            if (config.isSyncStats()) {
                long floor = now + progress.amount(ADJUST + stat.key());
                if (floor > progress.amount(stat.key())) progress.set(stat.key(), floor);
            }
        }
    }

    private static void trainFrom(TrackedPlayer progress, ProgressionStat stat, long gained) {
        if (gained <= 0) return;
        switch (stat) {
            case KILLS:
            case MOB_KILLS:
                progress.add(Skill.COMBAT.key(), gained);
                break;
            case FISH_CAUGHT:
                progress.add(Skill.FISHING.key(), gained);
                break;
            case ITEMS_ENCHANTED:
                progress.add(Skill.ENCHANTING.key(), gained);
                break;
            case ITEMS_CRAFTED:
                progress.add(Skill.CRAFTING.key(), gained);
                break;
            default:
                break;
        }
    }

    /**
     * First refresh after a player's progress loads: sets the baselines and floors, and starts the
     * star count from where they are, so stars gained before or while joining are not announced.
     */
    public static void joined(CosmicPlayer player) {
        refresh(player);
        getLoaded(player.getUuid()).ifPresent(progress -> {
            long stars = stars(progress);
            if (stars > progress.amount(LAST_STAR)) progress.set(LAST_STAR, stars);
        });
    }

    // ---- star-ups ------------------------------------------------------------------------------

    /**
     * Announces new stars to online players. Statistic-backed categories change without an event,
     * so stars are found by polling rather than on each gain.
     */
    public static void checkStars() {
        if (! isEnabled()) return;
        for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) {
            TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
            if (progress == null || ! progress.isFullyLoaded()) continue;

            long stars = stars(progress);
            long last = progress.amount(LAST_STAR);
            if (stars > last) {
                progress.set(LAST_STAR, stars);
                ModuleUtils.sendMessage(player, message("star-up").replace("%stars%", String.valueOf(stars)));
            } else if (stars < last) {
                // Weights went down or staff removed XP; announce again only on stars above this.
                progress.set(LAST_STAR, stars);
            }
        }
    }

    // ---- staff changes -------------------------------------------------------------------------

    /**
     * Sets a category to {@code change(current)}, never below zero, and completes with the new
     * amount: live for a player loaded here, otherwise in the database. For a statistic-backed
     * category the difference from the statistic is kept, so a later sync does not undo it.
     */
    public static CompletableFuture<Long> modify(String uuid, ProgressionStat stat, LongUnaryOperator change) {
        Boards.clear(Track.PROGRESSION);
        Optional<TrackedPlayer> live = getLoaded(uuid);
        if (live.isPresent()) {
            long next = apply(live.get(), stat, change);
            live.get().save(true);
            return CompletableFuture.completedFuture(next);
        }
        return snapshot(uuid).thenApply(stored -> {
            long next = apply(stored, stat, change);
            Track.PROGRESSION.getKeeper().save(stored, false);
            return next;
        });
    }

    private static long apply(TrackedPlayer progress, ProgressionStat stat, LongUnaryOperator change) {
        long current = progress.amount(stat.key());
        long next = Math.max(0L, change.applyAsLong(current));
        Long figure = progress.getBaselines().get(stat.key());
        if (stat.hasVanilla() && figure != null) {
            progress.set(ADJUST + stat.key(), next - figure);
        } else if (stat.hasVanilla()) {
            progress.set(ADJUST + stat.key(), progress.amount(ADJUST + stat.key()) + next - current);
        }
        progress.set(stat.key(), next);
        return next;
    }

    /**
     * Raises one online player's statistic-backed categories to their statistics now, whether or
     * not {@code sync-stats} is on. Waits for the server thread, so call it off that thread.
     */
    public static boolean sync(CosmicPlayer player) {
        TrackedPlayer progress = getLoaded(player.getUuid()).orElse(null);
        if (progress == null || ! progress.isFullyLoaded()) return false;
        Map<ProgressionStat, Long> figures = readVanilla(player.getUuid());
        for (Map.Entry<ProgressionStat, Long> entry : figures.entrySet()) {
            String key = entry.getKey().key();
            progress.getBaselines().put(key, entry.getValue());
            long floor = entry.getValue() + progress.amount(ADJUST + key);
            if (floor > progress.amount(key)) progress.set(key, floor);
        }
        Boards.clear(Track.PROGRESSION);
        return true;
    }

    // ---- boards --------------------------------------------------------------------------------

    /** Top players by one category, or by total weighted XP when {@code stat} is null. */
    public static CompletableFuture<List<TrackKeeper.Ranked>> board(ProgressionStat stat) {
        if (stat == null) return Boards.weighted(Track.PROGRESSION, "total", weightsByKey());
        List<String> ids = new ArrayList<>();
        ids.add(stat.key());
        return Boards.top(Track.PROGRESSION, "id:" + stat.key(), ids);
    }

    // ---- display -------------------------------------------------------------------------------

    /** The chat star prefix for a sender loaded here, or empty when chat stars are off. */
    public static String chatPrefix(CosmicSender sender) {
        if (! isEnabled() || ! config.isChatStarsEnabled() || sender == null || sender.isConsole()) return "";
        return getLoaded(sender.getUuid())
                .map(progress -> message("chat-prefix").replace("%stars%", String.valueOf(stars(progress))))
                .orElse("");
    }

    public static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001D) return String.format(Locale.ROOT, "%,d", (long) Math.rint(value));
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    public static String message(String key) {
        return config.getMessage(key);
    }

    /** The categories' keys, for tab completion. */
    public static Collection<String> categoryKeys() {
        List<String> keys = new ArrayList<>();
        for (ProgressionStat stat : ProgressionStat.values()) keys.add(stat.key());
        return keys;
    }
}
