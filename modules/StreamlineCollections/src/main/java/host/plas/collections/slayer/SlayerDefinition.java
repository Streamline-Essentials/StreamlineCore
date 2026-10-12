package host.plas.collections.slayer;

import lombok.Getter;

/**
 * One mob's slayer track: which entity counts toward it and the kills that complete each level.
 * Level {@code n} is complete at {@code tiers[n - 1]} kills.
 */
@Getter
public class SlayerDefinition {
    /** The track id: the entity's id without the {@code minecraft:} namespace, such as {@code zombie}. */
    private final String id;
    /** The entity's namespaced id, as kill events report it. */
    private final String entityType;
    private final SlayerCategory category;
    private final String icon;
    private final String displayName;
    private final long[] tiers;

    public SlayerDefinition(String id, String entityType, SlayerCategory category, String icon, String displayName, long[] tiers) {
        this.id = id;
        this.entityType = entityType;
        this.category = category;
        this.icon = icon;
        this.displayName = displayName;
        this.tiers = tiers;
    }

    /** The completed level for {@code kills}, from 0 to {@link #levels()}. */
    public int tier(long kills) {
        int tier = 0;
        for (long threshold : tiers) {
            if (kills < threshold) break;
            tier++;
        }
        return tier;
    }

    public int levels() {
        return tiers.length;
    }

    public boolean maxed(long kills) {
        return tiers.length > 0 && kills >= tiers[tiers.length - 1];
    }

    /** The kills that complete {@code level} (1-based), or 0 outside the range. */
    public long threshold(int level) {
        return level < 1 || level > tiers.length ? 0L : tiers[level - 1];
    }

    /** The kills for the next level, or the last level's kills once maxed. */
    public long next(long kills) {
        for (long threshold : tiers) {
            if (kills < threshold) return threshold;
        }
        return tiers.length == 0 ? 0L : tiers[tiers.length - 1];
    }
}
