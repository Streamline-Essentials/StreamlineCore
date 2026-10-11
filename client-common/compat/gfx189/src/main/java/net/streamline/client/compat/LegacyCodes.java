package net.streamline.client.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the color-coded strings {@code Canvas} takes into what Minecraft 1.8.9's font draws:
 * {@code §} codes only. Hex colors ({@code {#rrggbb}}, {@code &#rrggbb}) have no 1.8.9
 * equivalent and become the nearest of the sixteen legacy colors.
 */
public final class LegacyCodes {

    private static final Pattern HEX = Pattern.compile("(?i)\\{#([0-9a-f]{6})}|&#([0-9a-f]{6})");
    private static final Pattern AMPERSAND = Pattern.compile("(?i)&([0-9a-fk-or])");
    private static final int CACHE_LIMIT = 512;
    private static final Map<String, String> CONVERTED = new ConcurrentHashMap<String, String>();

    /** The legacy colors' RGB values, by their code character. */
    private static final char[] CODES = "0123456789abcdef".toCharArray();
    private static final int[] RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
    };

    private LegacyCodes() {}

    public static String convert(String text) {
        if (text == null || text.isEmpty()) return "";
        String cached = CONVERTED.get(text);
        if (cached != null) return cached;

        Matcher hex = HEX.matcher(text);
        StringBuffer out = new StringBuffer();
        while (hex.find()) {
            String digits = hex.group(1) != null ? hex.group(1) : hex.group(2);
            hex.appendReplacement(out, "§" + nearest(Integer.parseInt(digits, 16)));
        }
        hex.appendTail(out);
        String converted = AMPERSAND.matcher(out.toString()).replaceAll("§$1");

        if (CONVERTED.size() >= CACHE_LIMIT) CONVERTED.clear();
        CONVERTED.put(text, converted);
        return converted;
    }

    /** @return the code character of the legacy color closest to {@code rgb} */
    static char nearest(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < RGB.length; i++) {
            int dr = r - ((RGB[i] >> 16) & 0xFF);
            int dg = g - ((RGB[i] >> 8) & 0xFF);
            int db = b - (RGB[i] & 0xFF);
            long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return CODES[best];
    }
}
