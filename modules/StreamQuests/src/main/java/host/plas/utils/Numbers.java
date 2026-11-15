package host.plas.utils;

import java.util.Locale;

/** Display formatting shared by the quest shop. */
public final class Numbers {
    private Numbers() {
    }

    /** Whole numbers with separators ({@code 1,500}), anything else with two decimals. */
    public static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) return String.format(Locale.ENGLISH, "%,d", (long) value);
        return String.format(Locale.ENGLISH, "%,.2f", value);
    }

    /** {@code minecraft:heavy_core} or {@code BREAK_BLOCK} as {@code Heavy Core} / {@code Break Block}. */
    public static String title(String raw) {
        if (raw == null) return "";
        String text = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        int components = text.indexOf('[');
        if (components < 0) components = text.indexOf('{');
        if (components >= 0) text = text.substring(0, components);
        StringBuilder out = new StringBuilder();
        for (String word : text.toLowerCase(Locale.ROOT).split("[_\\s]+")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** 1 to 10 as roman numerals, anything else as digits. */
    public static String roman(int level) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= numerals.length ? numerals[level - 1] : String.valueOf(level);
    }

    /** Seconds as {@code 45s}, {@code 3m}, {@code 2m 30s} or {@code 1h 5m}. */
    public static String duration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int rest = seconds % 60;
        StringBuilder out = new StringBuilder();
        if (hours > 0) out.append(hours).append("h");
        if (minutes > 0) out.append(out.length() > 0 ? " " : "").append(minutes).append("m");
        if (rest > 0 || out.length() == 0) out.append(out.length() > 0 ? " " : "").append(rest).append("s");
        return out.toString();
    }
}
