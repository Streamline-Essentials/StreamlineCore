package singularity.text;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the delimiters used to mark hex-colour codes in a text string.
 *
 * <p>A {@code HexPolicy} wraps a {@code starter} and {@code ender} string that together
 * delimit a six-digit hex colour value (e.g., {@code {#FF0000}}).  Multiple policies
 * can be registered with {@link TextManager} so that several syntaxes can be supported
 * simultaneously.
 */
@Setter
@Getter
public class HexPolicy implements Comparable<HexPolicy> {

    /** The opening delimiter that precedes the six-digit hex value. */
    private String starter;

    /** The closing delimiter that follows the six-digit hex value. */
    private String ender;

    /**
     * Creates a {@code HexPolicy} with custom delimiters.
     *
     * @param starter the opening delimiter string
     * @param ender   the closing delimiter string
     */
    public HexPolicy(String starter, String ender) {
        this.starter = starter;
        this.ender = ender;
    }

    /**
     * Creates a {@code HexPolicy} with the default delimiters {@code {#} and {@code }}.
     */
    public HexPolicy() {
        this("{#", "}");
    }

    /**
     * Wraps the given six-digit hex string with the configured delimiters.
     *
     * @param hex a six-character hexadecimal colour string (e.g., {@code "FF0000"})
     * @return the delimited hex string (e.g., {@code "{#FF0000}"})
     */
    public String getResult(String hex) {
        return starter + hex + ender;
    }

    /**
     * Returns a canonical string that uniquely identifies this policy by its delimiters,
     * using a fixed dummy hex value of {@code "123456"}.  Used for equality checks in
     * sorted sets managed by {@link TextManager}.
     *
     * @return the identifiable string combining the starter, dummy hex, and ender
     */
    public String getIdentifiably() {
        return getStarter() + "123456" + getEnder();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Comparison is based on {@link #getIdentifiably()} so that policies with the
     * same delimiters are considered equal in sorted collections.
     */
    @Override
    public int compareTo(@NotNull HexPolicy o) {
        return getIdentifiably().compareTo(o.getIdentifiably());
    }
}