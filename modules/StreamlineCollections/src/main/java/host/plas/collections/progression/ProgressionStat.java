package host.plas.collections.progression;

import lombok.Getter;

import java.util.Locale;

/**
 * A progression category. Each one is worth {@code amount * weight} XP, with the weight read from
 * {@code progression.yml}.
 *
 * <p>A category is either <em>counted</em> by this module from gameplay events (chat, joins,
 * leaves, blocks broken and placed), or <em>read</em> from a vanilla statistic, divided by
 * {@code divisor} so distances read in blocks rather than centimetres. Read categories gain what
 * the statistic gained while the player was online on this server, so a network's servers add up
 * into one amount. {@link #getVanillaType()} on a counted category is the statistic it is floored
 * at when syncing.</p>
 */
@Getter
public enum ProgressionStat {
    BROKEN_BLOCKS(1.0D, "Broken Blocks", "minecraft:iron_pickaxe", true, "minecraft:mined", 1, "*"),
    PLACED_BLOCKS(0.5D, "Placed Blocks", "minecraft:grass_block", true, null, 1),
    KILLS(5.0D, "Player Kills", "minecraft:diamond_sword", false, "minecraft:custom", 1, "minecraft:player_kills"),
    MOB_KILLS(2.0D, "Mob Kills", "minecraft:zombie_head", false, "minecraft:custom", 1, "minecraft:mob_kills"),
    FISH_CAUGHT(10.0D, "Fish Caught", "minecraft:fishing_rod", false, "minecraft:custom", 1, "minecraft:fish_caught"),
    ITEMS_ENCHANTED(1.5D, "Items Enchanted", "minecraft:enchanting_table", false, "minecraft:custom", 1, "minecraft:enchant_item"),
    CHAT_MESSAGES(0.5D, "Chat Messages", "minecraft:oak_sign", true, null, 1),
    ITEMS_CRAFTED(10.0D, "Items Crafted", "minecraft:crafting_table", false, "minecraft:crafted", 1, "*"),
    JOINS(0.5D, "Joins", "minecraft:oak_door", true, null, 1),
    LEAVES(0.5D, "Leaves", "minecraft:iron_door", true, "minecraft:custom", 1, "minecraft:leave_game"),
    ELYTRA_FLIGHT(1.0D, "Elytra Flight", "minecraft:elytra", false, "minecraft:custom", 100, "minecraft:aviate_one_cm"),
    WALKING(1.0D, "Walking", "minecraft:leather_boots", false, "minecraft:custom", 100, "minecraft:walk_one_cm", "minecraft:sprint_one_cm"),
    SWIMMING(1.0D, "Swimming", "minecraft:water_bucket", false, "minecraft:custom", 100, "minecraft:swim_one_cm"),
    ITEMS_PICKED_UP(0.5D, "Items Picked Up", "minecraft:hopper", false, "minecraft:picked_up", 1, "*"),
    ITEMS_DROPPED(0.5D, "Items Dropped", "minecraft:dropper", false, "minecraft:custom", 1, "minecraft:drop"),
    ;

    private final double defaultWeight;
    private final String displayName;
    private final String icon;
    /** Whether this module counts the category from events rather than reading it from a statistic. */
    private final boolean counted;
    /** The vanilla statistic type read (or, for a counted category, floored at), or null for none. */
    private final String vanillaType;
    private final int divisor;
    /** The ids within {@link #vanillaType} that are summed; {@code *} sums every one. */
    private final String[] vanillaIds;

    ProgressionStat(double defaultWeight, String displayName, String icon, boolean counted, String vanillaType, int divisor,
                    String... vanillaIds) {
        this.defaultWeight = defaultWeight;
        this.displayName = displayName;
        this.icon = icon;
        this.counted = counted;
        this.vanillaType = vanillaType;
        this.divisor = Math.max(1, divisor);
        this.vanillaIds = vanillaIds;
    }

    /** The key used in {@code progression.yml}, placeholders, commands and the database. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Whether a vanilla statistic backs this category, read or as a floor. */
    public boolean hasVanilla() {
        return vanillaType != null;
    }

    /** Whether every id of {@link #vanillaType} is summed. */
    public boolean sumsEveryId() {
        return vanillaIds.length == 1 && vanillaIds[0].equals("*");
    }

    public static ProgressionStat byKey(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (ProgressionStat stat : values()) {
            if (stat.key().equals(key) || stat.displayName.equalsIgnoreCase(raw.trim())) return stat;
        }
        return null;
    }
}
