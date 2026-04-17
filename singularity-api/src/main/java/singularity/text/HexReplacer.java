package singularity.text;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.objects.AtomicString;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Scans a string for hex-colour codes enclosed by configurable delimiters and
 * rewrites them to a target format.
 *
 * <p>The replacement target ({@link #setTo}) may contain the placeholder
 * {@code %hex%}, which is substituted with the matched six-digit hex value.
 * For example, a {@code HexReplacer} with starter {@code "{#"}, ender {@code "}"},
 * and setTo {@code "<#%hex%>"} converts {@code {#FF0000}} to {@code <#FF0000>}.
 */
@Setter
@Getter
public class HexReplacer implements Comparable<HexReplacer> {

    /** Regex fragment that matches exactly six hexadecimal characters. */
    public static final String HEX_REGEX = "([0-9a-fA-F]{6})";

    /**
     * Template for the full regex pattern.  {@code %starter%} and {@code %ender%}
     * are replaced at runtime with the literal-escaped values of {@link #starter}
     * and {@link #ender}.
     */
    public static final String FULL_REGEX = "((%starter%)" + HEX_REGEX + "(%ender%))";

    /** The opening delimiter that precedes the six-digit hex value in the source text. */
    private String starter;

    /** The closing delimiter that follows the six-digit hex value in the source text. */
    private String ender;

    /**
     * The replacement template applied to each match.  The literal {@code %hex%}
     * inside this string is substituted with the captured hex digits.
     */
    private String setTo;

    /**
     * Creates a fully custom {@code HexReplacer}.
     *
     * @param starter the opening delimiter string
     * @param ender   the closing delimiter string
     * @param setTo   the replacement template (use {@code %hex%} as the hex placeholder)
     */
    public HexReplacer(String starter, String ender, String setTo) {
        this.starter = starter;
        this.ender = ender;
        this.setTo = setTo;
    }

    /**
     * Creates a {@code HexReplacer} with the default Streamline syntax:
     * input {@code {#RRGGBB}} is converted to {@code <#RRGGBB>}.
     */
    public HexReplacer() {
        this("{#", "}", "<#%hex%>");
    }

    /**
     * Wraps the given hex string with the configured starter and ender delimiters,
     * producing the source-side representation used during scanning.
     *
     * @param hex a six-character hexadecimal colour string
     * @return the delimited hex string
     */
    public String getWith(String hex) {
        return getStarter() + hex + getEnder();
    }

    /**
     * Builds the concrete regex pattern for this replacer by substituting the
     * literal-escaped {@link #starter} and {@link #ender} into {@link #FULL_REGEX}.
     *
     * @return the fully resolved regex string
     */
    public String getRegex() {
        return FULL_REGEX
                .replace("%starter%", MatcherUtils.makeLiteral(getStarter()))
                .replace("%ender%", MatcherUtils.makeLiteral(getEnder()))
                ;
    }

    /**
     * Scans the given string and returns all regex group matches for
     * {@link #getRegex()}.  Each element of the returned list is an array
     * of four capture groups: full match, whole colour token, hex digits, and ender.
     *
     * @param from the text to scan
     * @return a list of capture-group arrays for each matched hex token
     */
    public List<String[]> scan(String from) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), from);
        return MatcherUtils.getGroups(matcher, 4);
    }

    /**
     * Replaces all found {@link #getRegex()} with {@link #getSetTo()} where {@link #getSetTo()}'s "%hex%" is replaced with the found hex.
     * @param text The text to replace.
     */
    public AtomicString replace(AtomicString text) {
        scan(text.get()).forEach(group -> {
            String hex = group[2];
            String with = getSetTo().replace("%hex%", hex);
            text.set(text.get().replace(group[1], with));
        });

        return text;
    }

    /**
     * Returns a canonical string that uniquely identifies this replacer by its
     * delimiters, using a fixed dummy hex value of {@code "123456"}.  Used for
     * equality comparison in sorted collections.
     *
     * @return the identifiable string combining the starter, dummy hex, and ender
     */
    public String getIdentifiably() {
        return getStarter() + "123456" + getEnder();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Comparison is delegated to {@link #getIdentifiably()} so replacers with the
     * same delimiters are considered equal.
     */
    @Override
    public int compareTo(@NotNull HexReplacer o) {
        return getIdentifiably().compareTo(o.getIdentifiably());
    }
}
