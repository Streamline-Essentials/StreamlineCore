package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IGameplayHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Counts the collections that follow a vanilla statistic, such as playtime and distances.
 *
 * <p>When a player's progress loads, each statistic's current amount becomes their baseline;
 * at every save (and on quit) what the statistic gained since is added, announcing levels as
 * usual. Counting gains rather than copying the statistic keeps a network with several game
 * servers adding each server's share into the one shared amount. {@link StatSync} can still
 * raise an amount to this server's statistic, for play before the module was installed.</p>
 *
 * <p>Reading statistics waits for the server thread, so call these off it or on it, never
 * while holding something the server thread needs.</p>
 */
public final class StatFeeds {
    private StatFeeds() {
    }

    /**
     * The amount each statistic-fed collection stands for right now, by collection id: the
     * statistic divided by the collection's divisor. Empty off game servers.
     */
    public static Map<String, Long> read(String uuid) {
        Map<String, Long> amounts = new HashMap<>();
        List<CollectionDefinition> fed = CollectionManager.getCatalog().statFed();
        IGameplayHandler gameplay = Singularity.gameplay().orElse(null);
        if (fed.isEmpty() || gameplay == null) return amounts;

        Map<String, Set<String>> wanted = new HashMap<>();
        for (CollectionDefinition definition : fed) {
            wanted.computeIfAbsent(definition.getStatisticType(), k -> new HashSet<>()).add(definition.getStatistic());
        }
        Map<String, Map<String, Long>> values = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : wanted.entrySet()) {
            values.put(entry.getKey(), gameplay.statistics(uuid, entry.getKey(), entry.getValue()));
        }

        for (CollectionDefinition definition : fed) {
            Map<String, Long> ofType = values.get(definition.getStatisticType());
            long value = ofType == null ? 0L : ofType.getOrDefault(definition.getStatistic(), 0L);
            amounts.put(definition.getId(), definition.fromStatistic(value));
        }
        return amounts;
    }

    /** Sets a loaded player's baselines to their statistics now, so only what follows counts. */
    public static void baseline(CosmicPlayer player) {
        CollectionPlayer progress = CollectionManager.getLoaded(player.getUuid()).orElse(null);
        if (progress == null) return;
        progress.getBaselines().putAll(read(player.getUuid()));
    }

    /**
     * Adds what each statistic gained since the player's baseline and moves the baseline up.
     * A collection without a baseline yet only gets one.
     *
     * @return the levels completed
     */
    public static int refresh(CosmicPlayer player) {
        if (! StreamlineCollections.isTrackingServer()) return 0;
        CollectionPlayer progress = CollectionManager.getLoaded(player.getUuid()).orElse(null);
        if (progress == null || ! progress.isFullyLoaded()) return 0;

        int levels = 0;
        for (Map.Entry<String, Long> entry : read(player.getUuid()).entrySet()) {
            long now = entry.getValue();
            Long before = progress.getBaselines().put(entry.getKey(), now);
            if (before != null && now > before) levels += CollectionManager.add(player, entry.getKey(), now - before, true);
        }
        return levels;
    }
}
