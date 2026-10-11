package host.plas.hud;

import singularity.client.ClientInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The placeholders element text understands, written the RAT way: {@code %streamlinehud_<key>%}.
 * Game clients run without Streamline's RAT registry, so {@link Template} resolves them itself,
 * and only these keys plus the core's {@code %streamlineclient_<key>%} ones ({@link ClientValues});
 * anything else between percent signs stays as typed.
 *
 * <p>The table is built once and only read afterwards, from the render thread.</p>
 */
public final class Placeholders {

    public static final String IDENTIFIER = "streamlinehud";
    /** What comes before a key in a token. */
    static final String PREFIX = "%" + IDENTIFIER + "_";
    /** What comes before a key in one of the core's tokens. */
    static final String CLIENT_PREFIX = "%streamlineclient_";
    /** RAT's line break, honoured in element text. */
    public static final String NEWLINE = "%newline%";

    /** Reads one value for the frame being drawn. */
    @FunctionalInterface
    public interface Value {
        String get(ClientInfo info);
    }

    private static final Map<String, Value> VALUES = new LinkedHashMap<>();
    /** Token to meaning, in the order the menu lists them. */
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();
    /** {@link #DESCRIPTIONS} followed by the core's placeholders, built on first use. */
    private static volatile Map<String, String> allDescriptions;

    static {
        add("color_main", "main HUD color", info -> HudStyle.mainCode());
        add("color_text", "HUD text color", info -> HudStyle.textCode());
        add("fps", "frames per second", info -> Integer.toString(info.fps()));
        add("x", "X position", info -> Formats.number(info.x(), HudStyle.getCoordinateDecimals()));
        add("y", "Y position", info -> Formats.number(info.y(), HudStyle.getCoordinateDecimals()));
        add("z", "Z position", info -> Formats.number(info.z(), HudStyle.getCoordinateDecimals()));
        add("block_x", "block X", info -> Long.toString((long) Math.floor(info.x())));
        add("block_y", "block Y", info -> Long.toString((long) Math.floor(info.y())));
        add("block_z", "block Z", info -> Long.toString((long) Math.floor(info.z())));
        add("facing", "facing: North, East, South, West", info -> Formats.capitalize(info.facing()));
        add("yaw", "yaw in degrees", info -> Formats.number(info.yaw(), 1));
        add("pitch", "pitch in degrees", info -> Formats.number(info.pitch(), 1));
        add("biome", "biome name", info -> Formats.idName(info.biome()));
        add("biome_id", "biome id", ClientInfo::biome);
        add("dimension", "dimension name", info -> Formats.idName(info.dimension()));
        add("dimension_id", "dimension id", ClientInfo::dimension);
        add("ping", "ping in ms (empty when unknown)", info -> info.ping() < 0 ? "" : Integer.toString(info.ping()));
        add("server", "server address", ClientInfo::serverAddress);
        add("player", "your name", ClientInfo::playerName);
        add("clock", "real-world time", info -> Formats.realClock(HudStyle.isTwelveHourClock()));
        add("game_time", "in-game time", info -> Formats.gameClock(info.dayTime(), HudStyle.isTwelveHourClock()));
        add("day", "in-game day", info -> Long.toString(info.day()));
        add("memory_used", "memory used, MB", info -> Long.toString(Formats.memoryUsed()));
        add("memory_max", "memory available, MB", info -> Long.toString(Formats.memoryMax()));
        add("memory_percent", "memory used, percent", info -> Long.toString(Formats.memoryPercent()));
        DESCRIPTIONS.put(NEWLINE, "starts a new line");
    }

    private Placeholders() {}

    private static void add(String key, String description, Value value) {
        VALUES.put(key, value);
        DESCRIPTIONS.put(token(key), description);
    }

    /** @return {@code key} as it is written in element text */
    public static String token(String key) {
        return PREFIX + key + "%";
    }

    /** @return the value behind {@code key}, or {@code null} if there is no such placeholder */
    static Value get(String key) {
        return VALUES.get(key.toLowerCase(Locale.ROOT));
    }

    /** @return the core's value behind {@code key}, or {@code null} if it has none or is too old to have any */
    static Value getClient(String key) {
        try {
            return ClientValues.get(key);
        } catch (LinkageError e) {
            return null;
        }
    }

    /** @return every token with what it shows, for the menu */
    public static Map<String, String> descriptions() {
        Map<String, String> all = allDescriptions;
        if (all != null) return all;
        Map<String, String> built = new LinkedHashMap<>(DESCRIPTIONS);
        try {
            built.putAll(ClientValues.descriptions());
        } catch (LinkageError ignored) {
            // An older core: only this module's own placeholders.
        }
        all = Collections.unmodifiableMap(built);
        allDescriptions = all;
        return all;
    }
}
