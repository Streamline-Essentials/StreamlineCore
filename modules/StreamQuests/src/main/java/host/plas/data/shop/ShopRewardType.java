package host.plas.data.shop;

import java.util.Locale;
import java.util.Optional;

/** What a quest shop reward hands the buyer. */
public enum ShopRewardType {
    /** Runs commands, in the quests' {@code @c}/{@code @s}/{@code @u}/{@code @n} form; unprefixed ones run as the console. */
    COMMAND,
    /** Gives an item by namespaced id, vanilla or modded, through the server's {@code give}. */
    ITEM,
    /** Gives experience levels or points. */
    EXP,
    /** Sends the buyer a message. */
    MESSAGE,
    /** Sends every online player and the console a message. */
    BROADCAST,
    /** Applies a mob effect by namespaced id, vanilla or modded. */
    POTION,
    ;

    public static Optional<ShopRewardType> parse(String name) {
        if (name == null) return Optional.empty();
        String key = name.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        switch (key) {
            case "COMMANDS": return Optional.of(COMMAND);
            case "ITEMS": return Optional.of(ITEM);
            case "XP": case "EXPERIENCE": return Optional.of(EXP);
            case "EFFECT": case "POTION_EFFECT": return Optional.of(POTION);
            default:
                try {
                    return Optional.of(valueOf(key));
                } catch (IllegalArgumentException e) {
                    return Optional.empty();
                }
        }
    }
}
