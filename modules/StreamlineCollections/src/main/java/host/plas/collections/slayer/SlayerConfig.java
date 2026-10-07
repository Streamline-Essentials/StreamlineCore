package host.plas.collections.slayer;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.LevelReward;
import singularity.configs.ModularizedConfig;

import java.util.List;
import java.util.TreeMap;

/**
 * {@code slayer.yml}: whether slayer counts, syncing, level thresholds, rewards and messages.
 */
public class SlayerConfig extends ModularizedConfig {

    public SlayerConfig() {
        super(StreamlineCollections.getInstance(), "slayer.yml", true);
        init();
    }

    @Override
    public void init() {
        isEnabled();
        isSyncStats();
        isRewardsEnabled();
        isRemindOnJoin();
        getRemindIntervalMinutes();
    }

    public boolean isEnabled() {
        reloadResource();
        return getOrSetDefault("enabled", true);
    }

    public boolean isSyncStats() {
        reloadResource();
        return getOrSetDefault("sync-stats", true);
    }

    public long[] getTiers() {
        return tiers("tiers", SlayerCatalog.DEFAULT_TIERS);
    }

    public long[] getBossTiers() {
        return tiers("boss-tiers", SlayerCatalog.BOSS_TIERS);
    }

    private long[] tiers(String path, long[] fallback) {
        reloadResource();
        List<Long> list;
        try {
            list = getResource().getLongList(path);
        } catch (Exception e) {
            return fallback;
        }
        if (list == null || list.isEmpty()) return fallback;
        long[] tiers = new long[list.size()];
        for (int i = 0; i < tiers.length; i++) tiers[i] = list.get(i);
        return tiers;
    }

    public boolean isRewardsEnabled() {
        reloadResource();
        return getOrSetDefault("rewards.enabled", true);
    }

    public boolean isRemindOnJoin() {
        reloadResource();
        return getOrSetDefault("rewards.remind-on-join", true);
    }

    public int getRemindIntervalMinutes() {
        reloadResource();
        return Math.max(0, getOrSetDefault("rewards.remind-interval-minutes", 60));
    }

    public TreeMap<Integer, LevelReward> getRewards() {
        return LevelReward.parse(this, "rewards.levels");
    }

    public LevelReward getReward(int level) {
        return LevelReward.pick(getRewards(), level);
    }

    public String getMessage(String key) {
        reloadResource();
        String message = getResource().getString("messages." + key);
        return message == null ? "" : message;
    }
}
