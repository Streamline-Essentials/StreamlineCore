package singularity.placeholders.replaceables;

import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import singularity.placeholders.RATRegistry;
import singularity.placeholders.callbacks.PlaceholderCallback;
import singularity.placeholders.expansions.RATExpansion;

/**
 * A {@link GenericReplaceable} that is scoped to a named expansion identifier.
 *
 * <p>The identifier is prepended to the placeholder key (separated by the provided
 * separator or derived from the {@link RATExpansion}), so that replacements only
 * fire for that expansion's namespace.</p>
 */
@Getter
public class IdentifiedReplaceable extends GenericReplaceable {

    /**
     * The expansion identifier that scopes this replaceable (e.g. {@code "streamline"}).
     */
    private final String identifier;

    /**
     * Creates a literal replaceable scoped to the given identifier.
     *
     * @param identifier the namespace identifier prepended to the placeholder key
     * @param separator  the string placed between the identifier and {@code from}
     * @param from       the literal placeholder key (suffix after identifier + separator)
     * @param callback   the callback invoked when the placeholder is matched
     */
    public IdentifiedReplaceable(String identifier, String separator, String from, PlaceholderCallback callback) {
        super(identifier + separator + from, callback);
        this.identifier = identifier;
    }

    /**
     * Creates a literal replaceable using the identifier and full key derived from a
     * {@link RATExpansion}.
     *
     * @param expansion the expansion whose builder supplies the identifier and key prefix
     * @param from      the literal placeholder key suffix
     * @param callback  the callback invoked when the placeholder is matched
     */
    public IdentifiedReplaceable(RATExpansion expansion, String from, PlaceholderCallback callback) {
        super(RATRegistry.getLiteralWithExpansion(from, expansion), callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }

    /**
     * Creates a regex-based replaceable scoped to the given identifier.
     *
     * <p>The identifier portion is escaped to a literal, then concatenated with the
     * raw {@code regex} pattern.</p>
     *
     * @param identifier the namespace identifier prepended to the pattern
     * @param separator  the string placed between the identifier and the regex
     * @param regex      the regex pattern for the variable portion of the placeholder
     * @param groups     the number of capture groups expected in {@code regex}
     * @param callback   the callback invoked when the placeholder is matched
     */
    public IdentifiedReplaceable(String identifier, String separator, String regex, int groups, PlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(identifier + separator) + regex, groups, callback);
        this.identifier = identifier;
    }

    /**
     * Creates a regex-based replaceable using the identifier and pattern derived from a
     * {@link RATExpansion}.
     *
     * @param expansion the expansion whose builder supplies the identifier and key prefix
     * @param regex     the regex pattern for the variable portion of the placeholder
     * @param groups    the number of capture groups expected in {@code regex}
     * @param callback  the callback invoked when the placeholder is matched
     */
    public IdentifiedReplaceable(RATExpansion expansion, String regex, int groups, PlaceholderCallback callback) {
        super(RATRegistry.getRegexWithExpansion(regex, expansion), groups, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }
}
