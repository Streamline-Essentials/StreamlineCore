package singularity.client.color;

import java.util.Arrays;
import java.util.Locale;

/**
 * Chroma colors: a hue that runs round the rainbow over time, as {@code {#rrggbb}} codes for
 * {@code %streamlineclient_colors_chroma%} and {@code %streamlineclient_colors_chroma_<type>%}.
 * Speed, saturation, brightness and the default {@link Type} come from the Client Settings page.
 *
 * <p>Every reader at the same moment gets the same color, so separate HUD elements stay in step.
 * Codes are cached per type for {@link #FRAME_MILLIS}, since the HUD asks for them every frame.</p>
 */
public final class Chroma {

    /** How long one computed color is reused; about one frame at 60 FPS. */
    private static final long FRAME_MILLIS = 15L;

    /** How the brightness moves while the hue cycles. */
    public enum Type {
        /** Steady brightness; only the hue moves. */
        STATIC,
        /** Brightness eases down and back up, slowly, like breathing. */
        BREATHING,
        /** Brightness snaps up and fades, twice per hue cycle. */
        PULSING,
        ;

        /** @return the id used in placeholders and settings: {@code breathing} */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** @return the type with this id, or {@code null} */
        public static Type byId(String id) {
            if (id == null) return null;
            for (Type type : values()) {
                if (type.id().equalsIgnoreCase(id.trim())) return type;
            }
            return null;
        }
    }

    private static volatile Type defaultType = Type.STATIC;
    /** Seconds for the hue to go once round. */
    private static volatile double cycleSeconds = 5.0;
    private static volatile float saturation = 1.0f;
    private static volatile float brightness = 1.0f;

    private static final String[] CODES = new String[Type.values().length];
    private static final long[] COMPUTED_AT = new long[Type.values().length];

    private Chroma() {}

    /** @return the current color for the default type, as a {@code {#rrggbb}} code */
    public static String code() {
        return code(defaultType);
    }

    /** @return the current color for {@code type}, as a {@code {#rrggbb}} code */
    public static String code(Type type) {
        long now = System.currentTimeMillis();
        int index = type.ordinal();
        synchronized (CODES) {
            String cached = CODES[index];
            if (cached != null && now - COMPUTED_AT[index] < FRAME_MILLIS) return cached;
            String code = toCode(rgb(type, now));
            CODES[index] = code;
            COMPUTED_AT[index] = now;
            return code;
        }
    }

    /** @return the color for {@code type} at {@code millis}, as {@code 0xRRGGBB} */
    public static int rgb(Type type, long millis) {
        double cycle = cycleSeconds * 1000.0;
        double phase = (millis % (long) cycle) / cycle;
        float value = brightness;
        switch (type) {
            case BREATHING:
                // One breath per hue cycle, never fully dark.
                value *= (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.cos(phase * 2 * Math.PI)));
                break;
            case PULSING: {
                double beat = (phase * 2) % 1.0;
                value *= (float) (0.3 + 0.7 * Math.pow(1.0 - beat, 3));
                break;
            }
            default:
                break;
        }
        return hsbToRgb((float) phase, saturation, value);
    }

    /** {@code java.awt.Color.HSBtoRGB} without AWT, which a game client may not load. */
    static int hsbToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0: r = (int) (brightness * 255.0f + 0.5f); g = (int) (t * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); break;
                case 1: r = (int) (q * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); break;
                case 2: r = (int) (p * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (t * 255.0f + 0.5f); break;
                case 3: r = (int) (p * 255.0f + 0.5f); g = (int) (q * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); break;
                case 4: r = (int) (t * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); break;
                default: r = (int) (brightness * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (q * 255.0f + 0.5f); break;
            }
        }
        return (r << 16) | (g << 8) | b;
    }

    /** @return {@code rgb} as a {@code {#rrggbb}} color code */
    public static String toCode(int rgb) {
        String hex = Integer.toHexString(rgb & 0xFFFFFF);
        StringBuilder out = new StringBuilder(9).append("{#");
        for (int i = hex.length(); i < 6; i++) out.append('0');
        return out.append(hex).append('}').toString();
    }

    private static void invalidate() {
        synchronized (CODES) {
            Arrays.fill(CODES, null);
        }
    }

    public static Type getDefaultType() {
        return defaultType;
    }

    public static void setDefaultType(Type type) {
        if (type != null) defaultType = type;
    }

    public static void setCycleSeconds(double seconds) {
        cycleSeconds = Math.max(0.5, Math.min(60.0, seconds));
        invalidate();
    }

    public static void setSaturation(double value) {
        saturation = (float) Math.max(0.0, Math.min(1.0, value));
        invalidate();
    }

    public static void setBrightness(double value) {
        brightness = (float) Math.max(0.0, Math.min(1.0, value));
        invalidate();
    }
}
