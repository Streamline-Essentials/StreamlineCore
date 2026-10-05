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
        Catalog catalog = new Catalog();

        long[] defaultTiers = tiers("default-tiers");
        if (defaultTiers.length == 0) defaultTiers = FALLBACK_TIERS;

        for (String id : getResource().getStringList("mature-only")) catalog.getMatureOnly().add(Catalog.key(id));

        for (String id : getResource().singleLayerKeySet("categories")) {
            String path = "categories." + id;
            catalog.addCategory(new CollectionCategory(id.toLowerCase(Locale.ROOT),
                    string(path + ".name", id),
                    string(path + ".icon", "minecraft:chest"),
                    string(path + ".color", "#FFFFFF")));
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

            catalog.addDefinition(new CollectionDefinition(id, category,
                    string(path + ".icon", "minecraft:chest"), string(path + ".name", rawId), tiers));

            addSources(catalog.getBlocks(), path + ".blocks", id);
            addSources(catalog.getDrops(), path + ".drops", id);
            addSources(catalog.getFish(), path + ".fish", id);
            addSources(catalog.getBuckets(), path + ".buckets", id);
        }

        return catalog;
    }

    private void addSources(Map<String, String> sources, String path, String collection) {
        List<String> ids = getResource().getStringList(path);
        if (ids == null) return;
        for (String source : ids) {
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

    private String string(String path, String fallback) {
        String value = getResource().getString(path);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
