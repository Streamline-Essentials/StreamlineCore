package host.plas.collections.config;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.LevelReward;
import singularity.configs.ModularizedConfig;
import singularity.gui.CosmicItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code config.yml}: tracking toggles, saving, rewards, reminders and messages.
 */
public class CollectionsConfig extends ModularizedConfig {

    public CollectionsConfig() {
        super(StreamlineCollections.getInstance(), "config.yml", true);
        init();
    }

    @Override
    public void init() {
        isTrackBlockBreak();
        isTrackMobKills();
        isTrackFishing();
        isTrackBucketFill();
        isCountPlacedBlocks();
        isStatSyncEnabled();
        isStatSyncOnJoin();
        getStatSyncIntervalMinutes();
        for (String source : new String[] { "blocks", "drops", "fish", "buckets" }) getStatFor(source);
        getSaveIntervalSeconds();
        isRewardsEnabled();
        isRemindOnJoin();
        getRemindIntervalMinutes();
    }

    public boolean isTrackBlockBreak() {
        reloadResource();
        return getOrSetDefault("tracking.block-break", true);
    }

    public boolean isTrackMobKills() {
        reloadResource();
        return getOrSetDefault("tracking.mob-kills", true);
    }

    public boolean isTrackFishing() {
        reloadResource();
        return getOrSetDefault("tracking.fishing", true);
    }

    public boolean isTrackBucketFill() {
        reloadResource();
        return getOrSetDefault("tracking.bucket-fill", true);
    }

    public boolean isCountPlacedBlocks() {
        reloadResource();
        return getOrSetDefault("tracking.count-placed-blocks", false);
    }

    public boolean isStatSyncEnabled() {
        reloadResource();
        return getOrSetDefault("stat-sync.enabled", true);
    }

    public boolean isStatSyncOnJoin() {
        reloadResource();
        return getOrSetDefault("stat-sync.on-join", true);
    }

    public int getStatSyncIntervalMinutes() {
        reloadResource();
        return Math.max(0, getOrSetDefault("stat-sync.interval-minutes", 5));
    }

    /**
     * The vanilla statistic type a source type reads, such as {@code minecraft:mined}, or
     * {@code null} when that source is not synced.
     */
    public String getStatFor(String sourceType) {
        reloadResource();
        String fallback;
        switch (sourceType) {
            case "blocks":
                fallback = "mined";
                break;
            case "drops":
            case "fish":
                fallback = "picked_up";
                break;
            default:
                fallback = "none";
                break;
        }
        String stat = getOrSetDefault("stat-sync.sources." + sourceType, fallback);
        if (stat == null || stat.trim().isEmpty() || stat.trim().equalsIgnoreCase("none")) return null;
        return CosmicItem.normalizeKey(stat);
    }

    public int getSaveIntervalSeconds() {
        reloadResource();
        return Math.max(10, getOrSetDefault("save-interval-seconds", 60));
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

    /** The configured rewards, by level. */
    public TreeMap<Integer, LevelReward> getRewards() {
        reloadResource();
        TreeMap<Integer, LevelReward> rewards = new TreeMap<>();
        for (String key : singleLayerKeySet("rewards.levels")) {
            int level;
            try {
                level = Integer.parseInt(key.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            String path = "rewards.levels." + key;
            String description = getResource().getString(path + ".description");
            List<String> commands = getResource().getStringList(path + ".commands");
            rewards.put(level, new LevelReward(description == null ? "" : description,
                    commands == null ? new ArrayList<>() : commands));
        }
        return rewards;
    }

    /** The reward for {@code level}: its own entry, or the highest configured one below it. */
    public LevelReward getReward(int level) {
        TreeMap<Integer, LevelReward> rewards = getRewards();
        Map.Entry<Integer, LevelReward> entry = rewards.floorEntry(level);
        if (entry == null) entry = rewards.firstEntry();
        return entry == null ? LevelReward.NONE : entry.getValue();
    }

    public String getMessage(String key) {
        reloadResource();
        String message = getResource().getString("messages." + key);
        return message == null ? "" : message;
    }
}
