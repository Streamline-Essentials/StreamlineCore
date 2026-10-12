package singularity.utils.profiles;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads Floodgate's API when Floodgate is installed, without linking against it: the classes
 * are looked up by name, so servers without Floodgate never load them.
 *
 * <p>Floodgate runs on Spigot, Velocity and BungeeCord (and on the mod loaders through its
 * mod ports); its {@code FloodgateApi} is the same everywhere.</p>
 */
public final class FloodgateHook {
    private FloodgateHook() {
    }

    private static volatile boolean checked = false;
    private static Object api;
    private static Method isFloodgatePlayer;
    private static Method getPlayer;
    private static Method getPlayerPrefix;
    private static Class<?> playerType;

    private static synchronized void init() {
        if (checked) return;
        checked = true;
        try {
            Class<?> type = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            api = type.getMethod("getInstance").invoke(null);
            isFloodgatePlayer = type.getMethod("isFloodgatePlayer", UUID.class);
            getPlayer = type.getMethod("getPlayer", UUID.class);
            getPlayerPrefix = type.getMethod("getPlayerPrefix");
            playerType = Class.forName("org.geysermc.floodgate.api.player.FloodgatePlayer");
            // Floodgate sets its instance while enabling; asked earlier, look again next time.
            if (api == null) checked = false;
        } catch (Throwable e) {
            api = null;
        }
    }

    /** Whether Floodgate's API is available on this server. */
    public static boolean isPresent() {
        init();
        return api != null;
    }

    /** Whether Floodgate says {@code uuid} is a Bedrock player connected through Geyser right now. */
    public static boolean isFloodgatePlayer(UUID uuid) {
        if (! isPresent() || uuid == null) return false;
        try {
            return (boolean) isFloodgatePlayer.invoke(api, uuid);
        } catch (Throwable e) {
            return false;
        }
    }

    /** The prefix Floodgate puts in front of Bedrock usernames, such as {@code .}. */
    public static Optional<String> getPlayerPrefix() {
        if (! isPresent()) return Optional.empty();
        try {
            return Optional.ofNullable((String) getPlayerPrefix.invoke(api));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /** The Java-side username (prefixed gamertag) of an online Floodgate player. */
    public static Optional<String> getJavaUsername(UUID uuid) {
        return call(uuid, "getJavaUsername");
    }

    /** The Xbox gamertag of an online Floodgate player. */
    public static Optional<String> getGamertag(UUID uuid) {
        return call(uuid, "getUsername");
    }

    private static Optional<String> call(UUID uuid, String method) {
        if (! isPresent() || uuid == null) return Optional.empty();
        try {
            Object player = getPlayer.invoke(api, uuid);
            if (player == null) return Optional.empty();
            Object value = playerType.getMethod(method).invoke(player);
            return Optional.ofNullable(value == null ? null : value.toString());
        } catch (Throwable e) {
            return Optional.empty();
        }
    }
}
