package gg.drak.tacoessentials.chat;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Fills in PlaceholderAPI placeholders when PlaceholderAPI is installed (Spigot only). Reached
 * through reflection, so the module needs neither Bukkit nor PlaceholderAPI to load on other
 * platforms.
 */
final class PapiBridge {
    private PapiBridge() {
    }

    private static volatile boolean looked;
    private static Method getOfflinePlayer;
    private static Method setPlaceholders;

    /** {@code text} with PlaceholderAPI placeholders filled in for the player, or unchanged without PlaceholderAPI. */
    static String apply(String uuid, String text) {
        if (text == null || text.indexOf('%') < 0) return text;
        if (! looked) lookUp();
        if (setPlaceholders == null) return text;
        try {
            Object player = getOfflinePlayer.invoke(null, UUID.fromString(uuid));
            Object result = setPlaceholders.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (Throwable t) {
            return text;
        }
    }

    private static synchronized void lookUp() {
        if (looked) return;
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Class<?> offlinePlayer = Class.forName("org.bukkit.OfflinePlayer");
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            getOfflinePlayer = bukkit.getMethod("getOfflinePlayer", UUID.class);
            setPlaceholders = papi.getMethod("setPlaceholders", offlinePlayer, String.class);
        } catch (Throwable t) {
            getOfflinePlayer = null;
            setPlaceholders = null;
        }
        looked = true;
    }
}
