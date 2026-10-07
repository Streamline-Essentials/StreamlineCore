package host.plas.collections.config;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.Catalog;
import host.plas.collections.data.CollectionCategory;
import host.plas.collections.data.CollectionDefinition;
import singularity.configs.ModularizedConfig;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code collections.yml}: the categories, the collections and what feeds each one.
 */
public class CatalogConfig extends ModularizedConfig {
    private static final long[] FALLBACK_TIERS = {50L, 100L, 250L, 500L, 1_000L, 2_500L, 5_000L, 10_000L, 25_000L, 50_000L};

    public CatalogConfig() {
        super(StreamlineCollections.getInstance(), "collections.yml", true);
        init();
    }

    @Override
    public void init() {
    }

    /**
     * Reads the file into a fresh {@link Catalog}, skipping entries it cannot use. Keys are read
     * from the underlying YAML, which keeps file order; that order is the menu order.
     */
    public Catalog load() {
        reloadResource();
        upgrade();
        Catalog catalog = new Catalog();

        long[] defaultTiers = tiers("default-tiers");
        if (defaultTiers.length == 0) defaultTiers = FALLBACK_TIERS;

        for (String id : getResource().getStringList("mature-only")) catalog.getMatureOnly().add(Catalog.key(id));

        for (String id : getResource().singleLayerKeySet("categories")) {
            String path = "categories." + id;
            catalog.addCategory(new CollectionCategory(id.toLowerCase(Locale.ROOT),
                    string(path + ".name", id),
                    string(path + ".icon", "minecraft:chest"),
                    string(path + ".color", "#FFFFFF"),
                    bool(path + ".summable", true)));
        }

        for (String rawId : getResource().singleLayerKeySet("collections")) {
            String id = rawId.toLowerCase(Locale.ROOT);
            String path = "collections." + rawId;

            CollectionCategory category = catalog.getCategory(string(path + ".category", "")).orElse(null);
            if (category == null) {
                StreamlineCollections.getInstance().logWarning("Collection '" + rawId + "' names an unknown category; skipping it.");
                continue;
            }

            long[] tiers = tiers(path + ".tiers");
            if (tiers.length == 0) tiers = defaultTiers;

            CollectionDefinition definition = new CollectionDefinition(id, category,
                    string(path + ".icon", "minecraft:chest"), string(path + ".name", rawId), tiers);
            catalog.addDefinition(definition);

            String statistic = string(path + ".statistic", "");
            if (! statistic.isEmpty()) {
                definition.followStatistic(Catalog.key(string(path + ".statistic-type", "custom")), Catalog.key(statistic),
                        number(path + ".divisor", 1L));
            }

            addSources(catalog.getBlocks(), definition, path, "blocks");
            addSources(catalog.getDrops(), definition, path, "drops");
            addSources(catalog.getFish(), definition, path, "fish");
            addSources(catalog.getBuckets(), definition, path, "buckets");
        }

        return catalog;
    }

    private void addSources(Map<String, String> sources, CollectionDefinition definition, String path, String sourceType) {
        String collection = definition.getId();
        List<String> ids = getResource().getStringList(path + "." + sourceType);
        if (ids == null) return;
        for (String source : ids) {
            definition.addSource(sourceType, Catalog.key(source));
            String previous = sources.put(Catalog.key(source), collection);
            if (previous != null && ! previous.equals(collection)) {
                StreamlineCollections.getInstance().logWarning("'" + source + "' feeds both '" + previous
                        + "' and '" + collection + "'; only '" + collection + "' counts it.");
            }
        }
    }

    private long[] tiers(String path) {
        List<Long> list;
        try {
            list = getResource().getLongList(path);
        } catch (Exception e) {
            return new long[0];
        }
        if (list == null) return new long[0];
        long[] tiers = new long[list.size()];
        for (int i = 0; i < tiers.length; i++) tiers[i] = list.get(i);
        return tiers;
    }

    private boolean bool(String path, boolean fallback) {
        Object value = getResource().get(path);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean(((String) value).trim());
        return fallback;
    }

    private long number(String path, long fallback) {
        Object value = getResource().get(path);
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? fallback : Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---- upgrades of files written by older versions --------------------------------------------

    /** The layout {@code collections.yml} files written by this version have. */
    private static final int CATALOG_VERSION = 2;

    private static final long[] DISTANCE = {1_000, 5_000, 10_000, 25_000, 50_000, 100_000, 250_000, 500_000, 1_000_000, 2_500_000};
    private static final long[] SHORT_DISTANCE = {250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000, 100_000, 250_000};

    /** Miscellaneous collections added in catalog version 2: id, name, icon, statistic, divisor, tiers. */
    private static final Object[][] MISCELLANEOUS = {
            {"playtime", "Playtime (Hours)", "clock", "play_time", 72_000L, new long[]{1, 5, 10, 25, 50, 100, 250, 500, 1_000, 2_500}},
            {"jumps", "Jumps", "rabbit_foot", "jump", 1L, new long[]{500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000, 100_000, 250_000, 500_000}},
            {"walk_distance", "Walk Distance", "leather_boots", "walk_one_cm", 100L, DISTANCE},
            {"sprint_distance", "Sprint Distance", "sugar", "sprint_one_cm", 100L, DISTANCE},
            {"swim_distance", "Swim Distance", "turtle_helmet", "swim_one_cm", 100L, SHORT_DISTANCE},
            {"elytra_distance", "Elytra Distance", "elytra", "aviate_one_cm", 100L, DISTANCE},
            {"fly_distance", "Fly Distance", "feather", "fly_one_cm", 100L, SHORT_DISTANCE},
            {"sneak_distance", "Sneak Distance", "chainmail_boots", "crouch_one_cm", 100L, new long[]{100, 250, 500, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000, 100_000}},
    };

    /**
     * Brings a file from an older version up to {@link #CATALOG_VERSION}, adding what the
     * newer layout has without touching anything the server owner changed:
     * <ul>
     *     <li>2: the Miscellaneous category and its collections, where those ids are free; clay
     *     moves from Fishing (fished clay) to Mining (clay blocks broken), but only while it is
     *     still exactly the old default.</li>
     * </ul>
     */
    private void upgrade() {
        long version = number("catalog-version", 1L);
        if (version >= CATALOG_VERSION) return;

        if (version < 2) {
            if (getResource().singleLayerKeySet("categories").stream().noneMatch(id -> id.equalsIgnoreCase("miscellaneous"))) {
                write("categories.miscellaneous.name", "Miscellaneous");
                write("categories.miscellaneous.icon", "clock");
                write("categories.miscellaneous.color", "#D17BFF");
                write("categories.miscellaneous.summable", false);
            }

            java.util.Set<String> existing = getResource().singleLayerKeySet("collections");
            for (Object[] entry : MISCELLANEOUS) {
                String id = (String) entry[0];
                if (existing.contains(id)) continue;
                String path = "collections." + id;
                write(path + ".name", entry[1]);
                write(path + ".category", "miscellaneous");
                write(path + ".icon", entry[2]);
                write(path + ".statistic", entry[3]);
                write(path + ".divisor", entry[4]);
                List<Long> tiers = new java.util.ArrayList<>();
                for (long tier : (long[]) entry[5]) tiers.add(tier);
                write(path + ".tiers", tiers);
            }

            List<String> clayFish = getResource().getStringList("collections.clay.fish");
            if ("fishing".equalsIgnoreCase(string("collections.clay.category", ""))
                    && clayFish != null && clayFish.size() == 2 && clayFish.contains("clay_ball") && clayFish.contains("clay")
                    && (getResource().getStringList("collections.clay.blocks") == null
                        || getResource().getStringList("collections.clay.blocks").isEmpty())) {
                getResource().remove("collections.clay.fish");
                write("collections.clay.category", "mining");
                write("collections.clay.blocks", new java.util.ArrayList<>(List.of("clay")));
            }
        }

        write("catalog-version", CATALOG_VERSION);
        StreamlineCollections.getInstance().logInfo("Upgraded collections.yml to catalog version " + CATALOG_VERSION
                + " (Miscellaneous collections; clay counted from mining).");
    }

    private String string(String path, String fallback) {
        String value = getResource().getString(path);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
