package host.plas.collections.slayer;

import singularity.gui.CosmicItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every mob a player can slay, grouped into {@link SlayerCategory categories}. A track's id is the
 * entity's vanilla id ({@code zombie}, {@code ender_dragon}). Mobs a server's Minecraft version
 * does not have never gain kills there, and show a barrier where their spawn egg is missing.
 */
public final class SlayerCatalog {
    private SlayerCatalog() {
    }

    /** Levels complete at these kill counts for every mob but the bosses. */
    public static final long[] DEFAULT_TIERS = {10L, 50L, 100L, 250L, 500L, 1_000L, 2_500L, 5_000L, 10_000L, 25_000L};
    /** Bosses take far longer to find and kill, so their levels come much sooner. */
    public static final long[] BOSS_TIERS = {1L, 3L, 5L, 10L, 25L, 50L, 100L, 250L, 500L, 1_000L};

    private static volatile Map<String, SlayerDefinition> definitions = new LinkedHashMap<>();
    private static volatile Map<String, SlayerDefinition> byEntity = new HashMap<>();
    private static volatile Map<SlayerCategory, List<SlayerDefinition>> byCategory = new EnumMap<>(SlayerCategory.class);

    /**
     * Rebuilds the catalog with the given level thresholds. Spider sits with the monsters even
     * though it is neutral by day; iron and snow golems are built by players and never attack them
     * unprovoked, so they are passive.
     */
    public static synchronized void build(long[] tiers, long[] bossTiers) {
        Builder builder = new Builder();
        builder.define(SlayerCategory.MONSTERS, tiers, "zombie", "husk", "zombie_villager", "skeleton", "stray", "bogged",
                "parched", "creeper", "spider", "cave_spider", "enderman", "endermite", "silverfish", "slime", "sulfur_cube",
                "witch", "phantom", "shulker", "breeze", "creaking", "pillager", "vindicator", "evoker", "vex", "ravager",
                "illusioner");
        builder.define(SlayerCategory.NETHER, tiers, "blaze", "ghast", "magma_cube", "piglin", "piglin_brute",
                "zombified_piglin", "hoglin", "zoglin", "strider", "wither_skeleton");
        builder.define(SlayerCategory.AQUATIC, tiers, "cod", "salmon", "pufferfish", "tropical_fish", "squid", "glow_squid",
                "dolphin", "turtle", "axolotl", "tadpole", "guardian", "drowned", "nautilus", "zombie_nautilus");
        builder.define(SlayerCategory.PASSIVE, tiers, "chicken", "cow", "mooshroom", "pig", "sheep", "rabbit", "bat", "bee",
                "fox", "ocelot", "panda", "polar_bear", "goat", "frog", "sniffer", "armadillo", "allay", "villager",
                "wandering_trader", "iron_golem", "snow_golem", "copper_golem");
        builder.define(SlayerCategory.PETS, tiers, "wolf", "cat", "parrot", "horse", "donkey", "mule", "llama", "trader_llama",
                "camel", "camel_husk", "skeleton_horse", "zombie_horse", "happy_ghast");
        builder.define(SlayerCategory.BOSSES, bossTiers, "ender_dragon", "wither", "warden", "elder_guardian", "giant");

        definitions = builder.definitions;
        byEntity = builder.byEntity;
        byCategory = builder.byCategory;
    }

    private static final class Builder {
        private final Map<String, SlayerDefinition> definitions = new LinkedHashMap<>();
        private final Map<String, SlayerDefinition> byEntity = new HashMap<>();
        private final Map<SlayerCategory, List<SlayerDefinition>> byCategory = new EnumMap<>(SlayerCategory.class);

        private Builder() {
            for (SlayerCategory category : SlayerCategory.values()) byCategory.put(category, new ArrayList<>());
        }

        private void define(SlayerCategory category, long[] tiers, String... ids) {
            for (String id : ids) {
                String entity = "minecraft:" + id;
                SlayerDefinition definition = new SlayerDefinition(id, entity, category, icon(id), displayName(id), tiers);
                definitions.put(id, definition);
                byEntity.put(entity, definition);
                byCategory.get(category).add(definition);
            }
        }
    }

    /** The mob's spawn egg, or a stand-in for the few mobs without one. */
    static String icon(String id) {
        switch (id) {
            case "giant":
                return "minecraft:zombie_head";
            case "illusioner":
                return "minecraft:bow";
            default:
                return "minecraft:" + id + "_spawn_egg";
        }
    }

    /** {@code zombified_piglin} as "Zombified Piglin". */
    static String displayName(String id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** The mob by track id ({@code zombie}) or entity id ({@code minecraft:zombie}), any case; null when unknown. */
    public static SlayerDefinition get(String id) {
        if (id == null) return null;
        String key = id.trim().toLowerCase(Locale.ROOT);
        SlayerDefinition definition = definitions.get(key);
        return definition != null ? definition : byEntity.get(CosmicItem.normalizeKey(key));
    }

    /** The mob a kill of {@code entityType} (a namespaced id) counts toward, or null. */
    public static SlayerDefinition forEntity(String entityType) {
        return entityType == null ? null : byEntity.get(CosmicItem.normalizeKey(entityType));
    }

    public static Collection<SlayerDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public static List<String> ids() {
        return new ArrayList<>(definitions.keySet());
    }

    public static List<SlayerDefinition> in(SlayerCategory category) {
        return Collections.unmodifiableList(byCategory.getOrDefault(category, Collections.emptyList()));
    }

    /** Every mob's entity id, for reading the {@code minecraft:killed} statistic. */
    public static List<String> entityIds() {
        return new ArrayList<>(byEntity.keySet());
    }
}
