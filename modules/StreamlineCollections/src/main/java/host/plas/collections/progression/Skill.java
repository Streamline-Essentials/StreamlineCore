package host.plas.collections.progression;

import lombok.Getter;
import singularity.gui.CosmicItem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A cosmetic skill. Each matching action is worth one skill XP; levels are every
 * {@code skill-xp-per-level}.
 *
 * <p>Blocks are told apart by their ids, since block tags are not available on every platform:
 * crops and saplings train Farming, logs, wood and leaves Foraging, soils and sands Excavation, and
 * stone, ores and the like Mining. Combat, Fishing, Enchanting and Crafting train from what the
 * matching vanilla statistic gained.</p>
 */
@Getter
public enum Skill {
    MINING("Mining", "minecraft:iron_pickaxe"),
    FARMING("Farming", "minecraft:iron_hoe"),
    COMBAT("Combat", "minecraft:iron_sword"),
    FISHING("Fishing", "minecraft:fishing_rod"),
    FORAGING("Foraging", "minecraft:iron_axe"),
    ENCHANTING("Enchanting", "minecraft:enchanting_table"),
    CRAFTING("Crafting", "minecraft:crafting_table"),
    EXCAVATION("Excavation", "minecraft:iron_shovel"),
    ;

    private static final Set<String> CROPS = new HashSet<>(Arrays.asList(
            "wheat", "carrots", "potatoes", "beetroots", "nether_wart", "cocoa", "sugar_cane", "cactus", "pumpkin", "melon",
            "sweet_berry_bush", "cave_vines", "cave_vines_plant", "bamboo", "bamboo_sapling", "torchflower_crop",
            "pitcher_crop", "kelp", "kelp_plant", "chorus_flower", "chorus_plant", "pumpkin_stem", "melon_stem",
            "attached_pumpkin_stem", "attached_melon_stem"));
    private static final Set<String> SOILS = new HashSet<>(Arrays.asList(
            "dirt", "grass_block", "coarse_dirt", "rooted_dirt", "podzol", "mycelium", "dirt_path", "farmland", "mud",
            "muddy_mangrove_roots", "sand", "red_sand", "suspicious_sand", "gravel", "suspicious_gravel", "clay", "soul_sand",
            "soul_soil", "snow", "snow_block", "powder_snow"));
    private static final String[] MINERALS = {
            "stone", "ore", "deepslate", "netherrack", "basalt", "blackstone", "obsidian", "end_stone", "terracotta", "tuff",
            "calcite", "amethyst", "quartz", "brick", "prismarine", "copper", "granite", "diorite", "andesite", "magma_block",
            "glowstone", "dripstone", "sandstone", "purpur", "iron_block", "gold_block", "diamond_block", "emerald_block",
            "lapis_block", "redstone_block", "coal_block", "netherite_block", "ancient_debris", "ice", "concrete"};

    private final String displayName;
    private final String icon;

    Skill(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    /** The key holding this skill's XP. */
    public String key() {
        return "skill_" + name().toLowerCase(Locale.ROOT);
    }

    /** The skill breaking {@code blockId} trains, or null. */
    public static Skill forBreak(String blockId) {
        String path = path(blockId);
        if (path.isEmpty() || path.endsWith("air")) return null;
        if (farming(path)) return FARMING;
        if (foraging(path)) return FORAGING;
        if (SOILS.contains(path) || path.endsWith("concrete_powder")) return EXCAVATION;
        for (String mineral : MINERALS) {
            if (path.contains(mineral)) return MINING;
        }
        return null;
    }

    /** The skill placing {@code blockId} trains, or null. */
    public static Skill forPlace(String blockId) {
        String path = path(blockId);
        if (path.isEmpty()) return null;
        if (farming(path)) return FARMING;
        if (SOILS.contains(path) || path.endsWith("concrete_powder")) return EXCAVATION;
        return null;
    }

    public static Skill byName(String raw) {
        if (raw == null) return null;
        for (Skill skill : values()) {
            if (skill.name().equalsIgnoreCase(raw.trim()) || skill.displayName.equalsIgnoreCase(raw.trim())) return skill;
        }
        return null;
    }

    private static boolean farming(String path) {
        return CROPS.contains(path) || path.endsWith("_sapling") || path.endsWith("_propagule");
    }

    private static boolean foraging(String path) {
        return path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem") || path.endsWith("_hyphae")
                || path.endsWith("_leaves") || path.endsWith("_planks") || path.endsWith("mushroom_block")
                || path.equals("mangrove_roots") || path.equals("bamboo_block");
    }

    private static String path(String id) {
        return id == null ? "" : CosmicItem.keyPath(CosmicItem.normalizeKey(id));
    }
}
