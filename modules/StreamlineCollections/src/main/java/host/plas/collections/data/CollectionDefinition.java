package host.plas.collections.data;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One collection: what it is called, where it shows, and the amounts each level needs.
 * Level {@code n} is complete once the collected amount reaches {@code tiers[n - 1]}.
 */
@Getter
public class CollectionDefinition {
    private final String id;
    private final CollectionCategory category;
    private final String icon;
    private final String displayName;
    private final long[] tiers;
    /** Source ids by source type ({@code blocks}, {@code drops}, {@code fish}, {@code buckets}). */
    private final Map<String, List<String>> sources = new LinkedHashMap<>();

    public CollectionDefinition(String id, CollectionCategory category, String icon, String displayName, long[] tiers) {
        this.id = id;
        this.category = category;
        this.icon = icon;
        this.displayName = displayName;
        this.tiers = tiers;
    }

    public void addSource(String sourceType, String id) {
        sources.computeIfAbsent(sourceType, k -> new ArrayList<>()).add(id);
    }

    /** The number of levels complete at {@code amount}. */
    public int tier(long amount) {
        int tier = 0;
        for (long threshold : tiers) {
            if (amount < threshold) break;
            tier++;
        }
        return tier;
    }

    public int levels() {
        return tiers.length;
    }

    public boolean maxed(long amount) {
        return tiers.length > 0 && amount >= tiers[tiers.length - 1];
    }

    /** The amount level {@code level} needs, or {@code 0} outside the levels. */
    public long threshold(int level) {
        return level < 1 || level > tiers.length ? 0L : tiers[level - 1];
    }

    /** The amount the next level needs, or the last level's when maxed. */
    public long next(long amount) {
        for (long threshold : tiers) {
            if (amount < threshold) return threshold;
        }
        return tiers.length == 0 ? 0L : tiers[tiers.length - 1];
    }
}
