package singularity.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finding web links in text and turning them into URLs a client accepts as a click target.
 *
 * <p>Clients only open {@code http} and {@code https} URLs with a host, and some versions drop
 * the whole message when a click event carries anything else, so every click value goes
 * through {@link #toClickUrl(String)} first.</p>
 */
public final class Links {
    private Links() {
    }

    /**
     * Where a link may start: at the beginning, after anything that cannot be part of a word,
     * domain or path, or right after a colour code such as {@code &b} or {@code &o}.
     */
    private static final String START = "(?<=^|[^a-z0-9.@/_&§-]|[&§][0-9a-fk-orx])";

    /** Links with a scheme ({@code https://...}) or starting with {@code www.}. */
    private static final Pattern EXPLICIT = Pattern.compile("(?i)" + Links.START + "(?:https?://|www\\.)[^\\s<>\"']+");

    /**
     * Explicit links, plus bare domains such as {@code example.com/page}. The top-level
     * domain must be letters only, two or more of them, so decimals ({@code 1.5}) never match.
     */
    private static final Pattern WITH_BARE = Pattern.compile(
            "(?i)" + Links.START + "(?:https?://[^\\s<>\"']+|www\\.[^\\s<>\"']+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,24}(?::\\d{1,5})?(?:/[^\\s<>\"']*)?)");

    /** Characters that usually end a sentence rather than a link. */
    private static final String TRAILING = ".,;:!?)]}*'\"";

    /** One link found in a piece of text. */
    public static final class Found {
        private final int start;
        private final int end;
        private final String text;
        private final String url;

        private Found(int start, int end, String text, String url) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.url = url;
        }

        /** Index of the link's first character in the searched text. */
        public int getStart() {
            return start;
        }

        /** Index just past the link's last character in the searched text. */
        public int getEnd() {
            return end;
        }

        /** The link as written. */
        public String getText() {
            return text;
        }

        /** The link as a click target, with {@code https://} added where it had no scheme. */
        public String getUrl() {
            return url;
        }
    }

    /**
     * Every link in {@code text}, in order. Punctuation closing a sentence is left out of a
     * link, and a closing bracket only stays when the link opened one.
     *
     * @param text        the text to search
     * @param bareDomains whether links without {@code https://} or {@code www.}, such as
     *                    {@code example.com}, count
     * @return the links that make a valid click target
     */
    public static List<Found> find(String text, boolean bareDomains) {
        List<Found> found = new ArrayList<>();
        if (text == null || text.isEmpty()) return found;

        Matcher matcher = (bareDomains ? WITH_BARE : EXPLICIT).matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            String raw = trimTrailing(text.substring(start, end));
            if (raw.isEmpty()) continue;
            String url = toClickUrl(raw);
            if (url == null) continue;
            found.add(new Found(start, start + raw.length(), raw, url));
        }
        return found;
    }

    /** Whether {@code text} holds at least one link. */
    public static boolean contains(String text, boolean bareDomains) {
        return ! find(text, bareDomains).isEmpty();
    }

    /**
     * {@code raw} as an {@code http}/{@code https} URL with a host, adding {@code https://}
     * when it has no scheme.
     *
     * @return the URL, or {@code null} when it is not one a client would open
     */
    public static String toClickUrl(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        if (! value.toLowerCase(Locale.ROOT).matches("^[a-z][a-z0-9+.-]*://.*")) value = "https://" + value;
        return isWebUrl(value) ? value : null;
    }

    /** Whether {@code url} is an {@code http} or {@code https} URL with a host. */
    public static boolean isWebUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null && ! uri.getHost().isEmpty();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String trimTrailing(String link) {
        String result = link;
        while (! result.isEmpty() && TRAILING.indexOf(result.charAt(result.length() - 1)) >= 0) {
            char last = result.charAt(result.length() - 1);
            if (last == ')' && count(result, '(') >= count(result, ')')) break;
            if (last == ']' && count(result, '[') >= count(result, ']')) break;
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static int count(String text, char c) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == c) n++;
        return n;
    }
}
