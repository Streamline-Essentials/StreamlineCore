package host.plas.hud;

import java.time.LocalTime;

/**
 * Text formatting for HUD lines. Runs every frame, so it builds strings by hand instead of
 * going through {@code String.format}.
 */
public final class Formats {

    private static final long MEGABYTE = 1024L * 1024L;
    /** Memory figures change slowly and reading them is not free, so they refresh once a second. */
    private static final long MEMORY_REFRESH_MILLIS = 1000L;

    private static volatile String memoryLine = "";
    private static volatile long memoryReadAt;

    private Formats() {}

    /** @return {@code value} rounded to {@code decimals} places (0..3), without exponent notation */
    public static String number(double value, int decimals) {
        if (decimals <= 0) return Long.toString(Math.round(value));
        long scale = decimals == 1 ? 10 : decimals == 2 ? 100 : 1000;
        long scaled = Math.round(Math.abs(value) * scale);
        StringBuilder out = new StringBuilder(12);
        if (value < 0 && scaled != 0) out.append('-');
        out.append(scaled / scale).append('.');
        String fraction = Long.toString(scaled % scale);
        for (int i = fraction.length(); i < decimals; i++) out.append('0');
        return out.append(fraction).toString();
    }

    /** @return the path of a namespaced id as words: {@code minecraft:dark_forest} → {@code Dark Forest} */
    public static String idName(String id) {
        if (id == null || id.isEmpty()) return "";
        String path = id.substring(id.indexOf(':') + 1);
        StringBuilder out = new StringBuilder(path.length());
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/') {
                out.append(' ');
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    /** @return {@code north} as {@code North} */
    public static String capitalize(String word) {
        if (word == null || word.isEmpty()) return "";
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /** @return the time as {@code 14:05} or {@code 2:05 PM} */
    public static String clock(int hour, int minute, boolean twelveHour) {
        StringBuilder out = new StringBuilder(8);
        if (twelveHour) {
            int shown = hour % 12 == 0 ? 12 : hour % 12;
            out.append(shown);
        } else {
            if (hour < 10) out.append('0');
            out.append(hour);
        }
        out.append(':');
        if (minute < 10) out.append('0');
        out.append(minute);
        if (twelveHour) out.append(hour < 12 ? " AM" : " PM");
        return out.toString();
    }

    public static String realClock(boolean twelveHour) {
        LocalTime now = LocalTime.now();
        return clock(now.getHour(), now.getMinute(), twelveHour);
    }

    /**
     * @param dayTime world day time in ticks; tick 0 is 06:00
     */
    public static String gameClock(long dayTime, boolean twelveHour) {
        long ticks = ((dayTime % 24000L) + 24000L) % 24000L;
        int hour = (int) ((ticks / 1000L + 6L) % 24L);
        int minute = (int) ((ticks % 1000L) * 60L / 1000L);
        return clock(hour, minute, twelveHour);
    }

    /** @return the JVM's used and maximum heap, read at most once a second */
    public static String memory() {
        long now = System.currentTimeMillis();
        if (now - memoryReadAt >= MEMORY_REFRESH_MILLIS) {
            Runtime runtime = Runtime.getRuntime();
            long used = (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE;
            long max = runtime.maxMemory() / MEGABYTE;
            long percent = max > 0 ? used * 100L / max : 0L;
            memoryLine = "&7Memory: &f" + used + "&7/&f" + max + "MB &7(" + percent + "%)";
            memoryReadAt = now;
        }
        return memoryLine;
    }
}
