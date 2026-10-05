package singularity.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brings the hex colour forms Streamline accepts — {@code &#RRGGBB}, {@code {#RRGGBB}},
 * {@code <#RRGGBB>} and a bare {@code #RRGGBB}, as BukkitOfUtils' {@code ColorUtils} reads them on
 * Spigot — down to the single form {@code &#RRGGBB}, so every platform's messenger only has to
 * understand that one.
 */
public final class HexColors {
    private HexColors() {
    }

    private static final Pattern WRAPPED = Pattern.compile("\\{#([A-Fa-f0-9]{6})}|<#([A-Fa-f0-9]{6})>");
    // A bare #RRGGBB not already written as &#RRGGBB.
    private static final Pattern BARE = Pattern.compile("(?<!&)#([A-Fa-f0-9]{6})");
    private static final Pattern AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String normalize(String text) {
        if (text == null || text.indexOf('#') < 0) return text;

        Matcher wrapped = WRAPPED.matcher(text);
        StringBuffer out = new StringBuffer();
        while (wrapped.find()) {
            String hex = wrapped.group(1) != null ? wrapped.group(1) : wrapped.group(2);
            wrapped.appendReplacement(out, "&#" + hex);
        }
        wrapped.appendTail(out);

        return BARE.matcher(out.toString()).replaceAll("&#$1");
    }

    /**
     * Rewrites every hex colour as Bukkit/BungeeCord's legacy {@code §x§R§R§G§G§B§B}, for
     * platforms whose legacy-text parser reads only that form.
     */
    public static String toSectionX(String text) {
        String normalized = normalize(text);
        if (normalized == null || normalized.indexOf('#') < 0) return normalized;

        Matcher matcher = AMPERSAND.matcher(normalized);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
