package host.plas.data.require;

import java.util.Locale;

/**
 * The key a requirement value is counted and looked up under. Block and entity types arrive as
 * namespaced ids ({@code minecraft:stone}) from Streamline's gameplay events, while quest configs
 * and stored progress use Bukkit's names ({@code STONE}); both resolve to the Bukkit-style key,
 * so a quest reads the same on Spigot and the mod loaders. Modded ids keep their namespace
 * ({@code create:andesite_alloy} becomes {@code CREATE:ANDESITE_ALLOY}).
 */
public final class QuestKeys {
    private QuestKeys() {}

    public static String key(RequirementType type, String value) {
        if (value == null) return "";

        switch (type) {
            case BREAK_BLOCK:
            case PLACE_BLOCK:
            case KILL_ENTITY:
            case KILLED_BY_ENTITY:
                return typeKey(value);
            default:
                return value;
        }
    }

    public static String typeKey(String id) {
        String key = id.trim();
        if (key.startsWith("minecraft:")) key = key.substring("minecraft:".length());
        return key.toUpperCase(Locale.ROOT);
    }

    /**
     * The Bukkit environment name for a mod loader's dimension id, so {@code ENVIRONMENT_JOINS}
     * requirements written for Spigot ({@code NORMAL}, {@code NETHER}, {@code THE_END}) count there
     * too. Other dimensions count under their own id, as {@link #typeKey(String)} writes it.
     */
    public static String environmentOf(String dimensionId) {
        switch (dimensionId) {
            case "minecraft:overworld":
                return "NORMAL";
            case "minecraft:the_nether":
                return "NETHER";
            case "minecraft:the_end":
                return "THE_END";
            default:
                return typeKey(dimensionId);
        }
    }
}
