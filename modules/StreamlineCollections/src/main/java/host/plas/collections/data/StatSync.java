package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.config.CollectionsConfig;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IGameplayHandler;
import singularity.modules.ModuleUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Raises collections to match the server's vanilla statistics. Each collection's sources are
 * read through the statistic configured for their source type (blocks through
 * {@code minecraft:mined}, for example) and summed; a collection below that sum is raised to it.
 * Collections are never lowered, so the larger of the tracked amount and the statistics wins.
 */
public final class StatSync {
    private StatSync() {
    }

    /**
     * Syncs a player loaded on this server, returning the number of levels it completed, or
     * {@code -1} when the player could not be synced. Blocks while the platform reads the
     * statistics on its server thread, so call it off that thread.
     */
    public static int sync(CosmicPlayer player, boolean tellPlayer) {
        if (! StreamlineCollections.isTrackingServer()) return -1;
        IGameplayHandler gameplay = Singularity.gameplay().orElse(null);
        CollectionPlayer progress = CollectionManager.getLoaded(player.getUuid()).orElse(null);
        if (gameplay == null || progress == null || ! progress.isFullyLoaded()) return -1;

        CollectionsConfig config = StreamlineCollections.getMainConfig();
        Catalog catalog = CollectionManager.getCatalog();

        Map<String, String> statBySource = new HashMap<>();
        for (String sourceType : new String[] { "blocks", "drops", "fish", "buckets" }) {
            String stat = config.getStatFor(sourceType);
            if (stat != null) statBySource.put(sourceType, stat);
        }

        // Every id each statistic type is needed for, so each type is read once.
        Map<String, Set<String>> wanted = new HashMap<>();
        for (CollectionDefinition definition : catalog.all()) {
            for (Map.Entry<String, List<String>> source : definition.getSources().entrySet()) {
                String stat = statBySource.get(source.getKey());
                if (stat != null) wanted.computeIfAbsent(stat, k -> new HashSet<>()).addAll(source.getValue());
            }
        }
        Map<String, Map<String, Long>> values = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : wanted.entrySet()) {
            values.put(entry.getKey(), gameplay.statistics(player.getUuid(), entry.getKey(), entry.getValue()));
        }

        int levels = 0;
        for (CollectionDefinition definition : catalog.all()) {
            long total = 0;
            for (Map.Entry<String, List<String>> source : definition.getSources().entrySet()) {
                String stat = statBySource.get(source.getKey());
                if (stat == null) continue;
                Map<String, Long> ofStat = values.get(stat);
                if (ofStat == null) continue;
                for (String id : source.getValue()) total += ofStat.getOrDefault(id, 0L);
            }

            long current = progress.amount(definition.getId());
            if (total > current) levels += CollectionManager.add(player, definition.getId(), total - current, false);
        }

        // Statistic-fed collections are raised to what this server's statistic stands for.
        for (Map.Entry<String, Long> entry : StatFeeds.read(player.getUuid()).entrySet()) {
            long current = progress.amount(entry.getKey());
            if (entry.getValue() > current) levels += CollectionManager.add(player, entry.getKey(), entry.getValue() - current, false);
        }

        if (levels > 0) {
            CollectionManager.clearBoards();
            if (tellPlayer) {
                ModuleUtils.sendMessage(player, CollectionManager.message("stats-synced")
                        .replace("%levels%", String.valueOf(levels))
                        .replace("%count%", String.valueOf(CollectionManager.unclaimed(progress))));
            }
        }
        return levels;
    }
}
