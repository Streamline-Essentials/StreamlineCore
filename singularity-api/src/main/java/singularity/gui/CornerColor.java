package singularity.gui;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stained-glass corner accent colours for shell GUIs (see {@link GuiLayout#fillShell}), the same
 * sixteen colours and matching rules as BukkitOfUtils' {@code CornerColor}.
 */
public enum CornerColor {
    WHITE(0xF9, 0xFF, 0xFE),
    ORANGE(0xF9, 0x80, 0x1D),
    MAGENTA(0xC7, 0x4E, 0xBD),
    LIGHT_BLUE(0x3A, 0xB3, 0xDA),
    YELLOW(0xFF, 0xE7, 0x0E),
    LIME(0x80, 0xC7, 0x1F),
    PINK(0xF3, 0x8B, 0xAA),
    GRAY(0x9D, 0x9D, 0x97),
    LIGHT_GRAY(0xD0, 0xD0, 0xD0),
    CYAN(0x16, 0x9C, 0x9C),
    PURPLE(0x89, 0x32, 0xB8),
    BLUE(0x3C, 0x44, 0xAA),
    BROWN(0x83, 0x54, 0x32),
    GREEN(0x5E, 0x7C, 0x16),
    RED(0xB0, 0x2E, 0x26),
    BLACK(0x1D, 0x1D, 0x21);

    private static final Pattern HEX_PATTERN = Pattern.compile("(?:&#|#)([0-9A-Fa-f]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("[&§]x(?:[&§][0-9A-Fa-f]){6}");

    private final int red;
    private final int green;
    private final int blue;

    CornerColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /** The pane's item id, such as {@code minecraft:light_blue_stained_glass_pane}. */
    public String paneMaterial() {
        return "minecraft:" + name().toLowerCase(Locale.ROOT) + "_stained_glass_pane";
    }

    /**
     * The corner colour whose pane is closest to the first colour in {@code displayText}: a
     * {@code #RRGGBB} / {@code &#RRGGBB} hex, a {@code &x&R&R&G&G&B&B} hex, or a legacy
     * {@code &} code.
     *
     * @return the closest colour, or {@link #YELLOW} when the text holds none
     */
    public static CornerColor fromDisplayText(String displayText) {
        if (displayText == null || displayText.isBlank()) return YELLOW;

        int[] rgb = extractRgb(displayText);
        if (rgb == null) return YELLOW;
        return closestTo(rgb[0], rgb[1], rgb[2]);
    }

    public static CornerColor closestTo(int red, int green, int blue) {
        CornerColor closest = YELLOW;
        long bestDistance = Long.MAX_VALUE;

        for (CornerColor color : values()) {
            long dr = red - color.red;
            long dg = green - color.green;
            long db = blue - color.blue;
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = color;
            }
        }
        return closest;
    }

    private static int[] extractRgb(String text) {
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        if (hexMatcher.find()) return parseHex(hexMatcher.group(1));

        Matcher legacyHexMatcher = LEGACY_HEX_PATTERN.matcher(text);
        if (legacyHexMatcher.find()) {
            String match = legacyHexMatcher.group();
            StringBuilder builder = new StringBuilder(6);
            for (int i = 0; i < match.length(); i++) {
                char character = match.charAt(i);
                if (character == '&' || character == '§' || character == 'x' || character == 'X') continue;
                builder.append(character);
            }
            if (builder.length() == 6) return parseHex(builder.toString());
        }

        for (int i = 0; i < text.length() - 1; i++) {
            char marker = text.charAt(i);
            if (marker != '&' && marker != '§') continue;
            int[] legacy = legacyCodeRgb(Character.toLowerCase(text.charAt(i + 1)));
            if (legacy != null) return legacy;
        }

        return null;
    }

    private static int[] parseHex(String hex) {
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private static int[] legacyCodeRgb(char code) {
        switch (code) {
            case '0': return new int[]{0, 0, 0};
            case '1': return new int[]{0, 0, 170};
            case '2': return new int[]{0, 170, 0};
            case '3': return new int[]{0, 170, 170};
            case '4': return new int[]{170, 0, 0};
            case '5': return new int[]{170, 0, 170};
            case '6': return new int[]{255, 170, 0};
            case '7': return new int[]{170, 170, 170};
            case '8': return new int[]{85, 85, 85};
            case '9': return new int[]{85, 85, 255};
            case 'a': return new int[]{85, 255, 85};
            case 'b': return new int[]{85, 255, 255};
            case 'c': return new int[]{255, 85, 85};
            case 'd': return new int[]{255, 85, 255};
            case 'e': return new int[]{255, 255, 85};
            case 'f': return new int[]{255, 255, 255};
            default: return null;
        }
    }
}
