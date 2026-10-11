package singularity.client.placeholders;

import singularity.client.color.Chroma;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Placeholders StreamlineClient itself fills, written the RAT way:
 * {@code %streamlineclient_<key>%}. Game clients have no RAT registry, so client modules that
 * show text (StreamlineHUD's elements, for one) look these up here alongside their own.
 *
 * <p>Values are read every frame on the render thread, so each one is cheap.</p>
 */
public final class ClientPlaceholders {

    public static final String IDENTIFIER = "streamlineclient";
    /** What comes before a key in a token. */
    public static final String PREFIX = "%" + IDENTIFIER + "_";

    private static final Map<String, Supplier<String>> VALUES = new LinkedHashMap<>();
    /** Token to meaning, in the order a menu lists them. */
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        add("colors_chroma", "chroma color, default type (Client Settings)", Chroma::code);
        for (Chroma.Type type : Chroma.Type.values()) {
            add("colors_chroma_" + type.id(), "chroma color, " + type.id(), () -> Chroma.code(type));
        }
    }

    private ClientPlaceholders() {}

    private static void add(String key, String description, Supplier<String> value) {
        VALUES.put(key, value);
        DESCRIPTIONS.put(token(key), description);
    }

    /** @return {@code key} as it is written in text */
    public static String token(String key) {
        return PREFIX + key + "%";
    }

    /** @return the value behind {@code key} (the part after {@link #PREFIX}), or {@code null} if there is none */
    public static Supplier<String> get(String key) {
        return VALUES.get(key.toLowerCase(Locale.ROOT));
    }

    /** @return every token with what it shows */
    public static Map<String, String> descriptions() {
        return Collections.unmodifiableMap(DESCRIPTIONS);
    }

    /** @return {@code text} with every known {@code %streamlineclient_<key>%} replaced; unknown ones stay as typed */
    public static String apply(String text) {
        if (text == null || ! text.contains(PREFIX)) return text;
        StringBuilder out = new StringBuilder(text.length() + 16);
        int i = 0;
        while (i < text.length()) {
            if (text.startsWith(PREFIX, i)) {
                int keyStart = i + PREFIX.length();
                int end = text.indexOf('%', keyStart);
                Supplier<String> value = end > keyStart ? get(text.substring(keyStart, end)) : null;
                if (value != null) {
                    String shown = value.get();
                    if (shown != null) out.append(shown);
                    i = end + 1;
                    continue;
                }
            }
            out.append(text.charAt(i++));
        }
        return out.toString();
    }
}
