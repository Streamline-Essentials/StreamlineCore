package host.plas.collections.progression;

import host.plas.collections.StreamlineCollections;
import lombok.Getter;
import singularity.configs.ModularizedConfig;

import java.util.EnumMap;
import java.util.Map;

/**
 * {@code progression.yml}. Values are read once per {@link #load()} and kept, because the star level
 * is worked out for every chat message and placeholder, where re-reading the file would be costly.
 */
@Getter
public class ProgressionConfig extends ModularizedConfig {
    private boolean enabled;
    private boolean syncStats;
    private int refreshIntervalSeconds;
    private double formulaMultiplier;
    private double formulaBase;
    private double maxNeeded;
    private boolean chatStarsEnabled;
    private double skillXpPerLevel;
    private final Map<ProgressionStat, Double> weights = new EnumMap<>(ProgressionStat.class);

    public ProgressionConfig() {
        super(StreamlineCollections.getInstance(), "progression.yml", true);
        init();
    }

    @Override
    public void init() {
        load();
    }

    /** Re-reads every setting from the file. */
    public void load() {
        reloadResource();
        enabled = getOrSetDefault("enabled", true);
        syncStats = getOrSetDefault("sync-stats", true);
        refreshIntervalSeconds = Math.max(5, getOrSetDefault("refresh-interval-seconds", 30));
        formulaMultiplier = nonNegative("formula-multiplier", 250.0D);
        formulaBase = nonNegative("formula-base", 500.0D);
        maxNeeded = nonNegative("max-needed", 5000.0D);
        chatStarsEnabled = getOrSetDefault("chat-stars-enabled", true);
        skillXpPerLevel = nonNegative("skill-xp-per-level", 1000.0D);

        Map<ProgressionStat, Double> read = new EnumMap<>(ProgressionStat.class);
        for (ProgressionStat stat : ProgressionStat.values()) read.put(stat, nonNegative("weights." + stat.key(), stat.getDefaultWeight()));
        synchronized (weights) {
            weights.clear();
            weights.putAll(read);
        }
    }

    public double weight(ProgressionStat stat) {
        Double weight = weights.get(stat);
        return weight == null ? stat.getDefaultWeight() : weight;
    }

    private double nonNegative(String path, double fallback) {
        Object raw = getResource().get(path);
        if (raw == null) {
            write(path, fallback);
            return fallback;
        }
        double value;
        try {
            value = raw instanceof Number ? ((Number) raw).doubleValue() : Double.parseDouble(raw.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
        return Double.isFinite(value) && value >= 0 ? value : fallback;
    }

    public String getMessage(String key) {
        String message = getResource().getString("messages." + key);
        return message == null ? "" : message;
    }
}
