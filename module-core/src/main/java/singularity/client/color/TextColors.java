package singularity.client.color;

/**
 * Rewrites the colors in HUD text, for Client Settings' Strip Colors and Chroma Everywhere.
 *
 * <p>It reads the same syntax the client draws: {@code &} and {@code §} codes, the hex forms
 * {@code &#rrggbb}, {@code {#rrggbb}}, {@code <#rrggbb>} and {@code #rrggbb}, and Bukkit's
 * {@code §x§r§r§g§g§b§b}. A color also resets bold, italic and the other decorations, so each
 * color is replaced by something that resets them too ({@code &r} or another color), which
 * keeps every decoration, and so the text's width, as it was.</p>
 *
 * <p>Called for every HUD line every frame, so it walks the text once and returns it untouched
 * when it holds no color.</p>
 */
public final class TextColors {

    private static final String RESET = "&r";

    private TextColors() {}

    /** @return {@code text} with every color code turned into a reset, so it draws in the base color */
    public static String strip(String text) {
        return replace(text, null, RESET);
    }

    /**
     * @param code a color code such as {@code {#ff0000}}
     * @return {@code text} in {@code code} throughout: it starts with it, and every color and
     * reset in it becomes it
     */
    public static String recolor(String text, String code) {
        return replace(text, code, code);
    }

    private static String replace(String text, String prefix, String replacement) {
        if (text == null) return null;
        StringBuilder out = null;
        int copied = 0;
        int length = text.length();
        int i = 0;
        while (i < length) {
            int end = colorEnd(text, i, prefix != null);
            if (end < 0) {
                i++;
                continue;
            }
            if (out == null) out = new StringBuilder(length + 16);
            out.append(text, copied, i).append(replacement);
            copied = i = end;
        }
        if (out == null) return prefix == null ? text : prefix + text;
        out.append(text, copied, length);
        return prefix == null ? out.toString() : prefix + out;
    }

    /**
     * @param resets whether {@code &r} counts as a color, as it does for recoloring, where a
     *               reset would otherwise bring back the base color
     * @return the end of the color starting at {@code i}, or {@code -1} if none starts there
     */
    private static int colorEnd(String text, int i, boolean resets) {
        char c = text.charAt(i);
        if (c == '&' && text.startsWith("&#", i) && isHex(text, i + 2)) return i + 8;
        if (c == '{' && text.startsWith("{#", i) && isHex(text, i + 2) && charAt(text, i + 8) == '}') return i + 9;
        if (c == '<' && text.startsWith("<#", i) && isHex(text, i + 2) && charAt(text, i + 8) == '>') return i + 9;
        if (c == '#' && isHex(text, i + 1)) return i + 7;
        if ((c == '&' || c == '§') && i + 1 < text.length()) {
            char code = Character.toLowerCase(text.charAt(i + 1));
            if (code == 'x' && isBukkitHex(text, i + 2)) return i + 14;
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) return i + 2;
            if (resets && code == 'r') return i + 2;
        }
        return -1;
    }

    private static boolean isHex(String text, int from) {
        if (from + 6 > text.length()) return false;
        for (int j = from; j < from + 6; j++) {
            if (Character.digit(text.charAt(j), 16) < 0) return false;
        }
        return true;
    }

    private static boolean isBukkitHex(String text, int from) {
        if (from + 12 > text.length()) return false;
        for (int j = 0; j < 6; j++) {
            char marker = text.charAt(from + j * 2);
            if ((marker != '§' && marker != '&') || Character.digit(text.charAt(from + j * 2 + 1), 16) < 0) return false;
        }
        return true;
    }

    private static char charAt(String text, int i) {
        return i < text.length() ? text.charAt(i) : '\0';
    }
}
