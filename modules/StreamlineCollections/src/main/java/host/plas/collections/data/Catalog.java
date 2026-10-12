package host.plas.collections.data;

import lombok.Getter;
import singularity.gui.CosmicItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every category and collection, in menu order, and which item or block feeds which collection.
 * Source ids are normalised namespaced ids ({@code minecraft:wheat}), matching what the gameplay
 * events report on every platform.
 */
@Getter
public class Catalog {
    private final Map<String, CollectionCategory> categories = new LinkedHashMap<>();
    private final Map<String, CollectionDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, String> blocks = new HashMap<>();
    private final Map<String, String> drops = new HashMap<>();
    private final Map<String, String> fish = new HashMap<>();
    private final Map<String, String> buckets = new HashMap<>();
    private final Set<String> matureOnly = new HashSet<>();

    public static String key(String id) {
        return CosmicItem.normalizeKey(id);
    }

    public void addCategory(CollectionCategory category) {
        categories.put(category.getId(), category);
    }

    public void addDefinition(CollectionDefinition definition) {
        definitions.put(definition.getId(), definition);
    }

    public Optional<CollectionDefinition> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(definitions.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<CollectionCategory> getCategory(String raw) {
        if (raw == null) return Optional.empty();
        for (CollectionCategory category : categories.values()) {
            if (category.getId().equalsIgnoreCase(raw) || category.getName().equalsIgnoreCase(raw)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    public List<CollectionDefinition> inCategory(CollectionCategory category) {
        List<CollectionDefinition> list = new ArrayList<>();
        for (CollectionDefinition definition : definitions.values()) {
            if (definition.getCategory() == category) list.add(definition);
        }
        return list;
    }

    public Collection<CollectionDefinition> all() {
        return definitions.values();
    }

    /** The collections that follow a vanilla statistic rather than gameplay events. */
    public List<CollectionDefinition> statFed() {
        List<CollectionDefinition> list = new ArrayList<>();
        for (CollectionDefinition definition : definitions.values()) {
            if (definition.isStatFed()) list.add(definition);
        }
        return list;
    }

    /** Every collection in a summable category: the ones that add up into the overall board. */
    public List<CollectionDefinition> summable() {
        List<CollectionDefinition> list = new ArrayList<>();
        for (CollectionDefinition definition : definitions.values()) {
            if (definition.getCategory().isSummable()) list.add(definition);
        }
        return list;
    }

    public String blockSource(String blockId) {
        return blocks.get(key(blockId));
    }

    public String dropSource(String itemId) {
        return drops.get(key(itemId));
    }

    public String fishSource(String itemId) {
        return fish.get(key(itemId));
    }

    public String bucketSource(String itemId) {
        return buckets.get(key(itemId));
    }

    public boolean needsMaturity(String blockId) {
        return matureOnly.contains(key(blockId));
    }
}
