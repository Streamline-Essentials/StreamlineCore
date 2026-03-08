package singularity.text;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a configurable hex color placeholder pattern.
 * Allows supporting multiple common formats like <#rrggbb>, &#rrggbb, {#rrggbb}, etc.
 */
@Getter
public class HexPolicy implements Comparable<HexPolicy> {

    private final String starter;
    private final String ender;
    private final Pattern pattern;           // Cached regex for fast matching
    private final String example;            // For sorting / debugging

    /**
     * Creates a new hex policy.
     *
     * @param starter opening part (e.g. "<#", "&#", "{#")
     * @param ender   closing part (usually ">" or "}")
     */
    public HexPolicy(@NotNull String starter, @NotNull String ender) {
        this.starter = Objects.requireNonNull(starter, "starter cannot be null");
        this.ender = Objects.requireNonNull(ender, "ender cannot be null");

        // Build a regex that captures exactly 6 hex digits after starter and before ender
        // Case-insensitive hex (a-f A-F 0-9)
        String regex = Pattern.quote(starter) + "([0-9a-fA-F]{6})" + Pattern.quote(ender);
        this.pattern = Pattern.compile(regex);

        // Used for compareTo / sorting / identification
        this.example = starter + "aabbcc" + ender;
    }

    /**
     * Default constructor – supports the popular MiniMessage / Adventure format <#rrggbb>
     */
    public HexPolicy() {
        this("<#", ">");
    }

    /**
     * Returns the full placeholder string for a given 6-digit hex code.
     * Example: for hex = "ff55aa" → returns "<#ff55aa>" (with default policy)
     */
    public String getResult(String hex) {
        if (hex == null || hex.length() != 6) {
            return starter + "ERROR" + ender;
        }
        return starter + hex.toLowerCase() + ender;
    }

    /**
     * Returns a representative string used for identification and sorting.
     */
    public String getIdentifiably() {
        return example;
    }

    /**
     * Returns the pre-compiled regex pattern that matches this hex format.
     * Use this in your TextManager / parser to find and replace hex codes.
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Checks if this policy matches the given input string (contains at least one match).
     */
    public boolean matches(String input) {
        return pattern.matcher(input).find();
    }

    @Override
    public int compareTo(@NotNull HexPolicy o) {
        return getIdentifiably().compareTo(o.getIdentifiably());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HexPolicy hexPolicy = (HexPolicy) o;
        return starter.equals(hexPolicy.starter) && ender.equals(hexPolicy.ender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(starter, ender);
    }

    @Override
    public String toString() {
        return "HexPolicy{starter='" + starter + "', ender='" + ender + "'}";
    }

    // -------------------------------------------------------------------------
    //  Common static factory methods for popular formats
    // -------------------------------------------------------------------------

    public static HexPolicy miniMessage() {
        return new HexPolicy("<#", ">");
    }

    public static HexPolicy ampersandHex() {
        return new HexPolicy("&#", "");
    }

    public static HexPolicy curlyBraces() {
        return new HexPolicy("{#", "}");
    }

    public static HexPolicy angleBracketsNoHash() {
        return new HexPolicy("<", ">");
    }

    // Legacy BungeeCord style (§x§r§r§g§g§b§b) – special case, not using starter/ender
    // Usually handled separately in parsers
}