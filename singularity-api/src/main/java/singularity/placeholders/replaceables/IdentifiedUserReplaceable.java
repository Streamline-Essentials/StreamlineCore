package singularity.placeholders.replaceables;

import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import singularity.placeholders.RATRegistry;
import singularity.placeholders.callbacks.UserPlaceholderCallback;
import singularity.placeholders.expansions.RATExpansion;

/**
 * A {@link UserReplaceable} that is scoped to a named expansion identifier.
 *
 * <p>Behaves identically to {@link IdentifiedReplaceable} but uses a
 * {@link UserPlaceholderCallback}, allowing the replacement logic to receive the
 * {@link singularity.data.console.CosmicSender} whose data should populate the
 * placeholder.</p>
 */
@Getter
public class IdentifiedUserReplaceable extends UserReplaceable {

    /**
     * The expansion identifier that scopes this replaceable (e.g. {@code "streamline"}).
     */
    private final String identifier;

    /**
     * Creates a literal user replaceable scoped to the given identifier.
     *
     * @param identifier the namespace identifier prepended to the placeholder key
     * @param separator  the string placed between the identifier and {@code from}
     * @param from       the literal placeholder key suffix
     * @param callback   the user-aware callback invoked when the placeholder is matched
     */
    public IdentifiedUserReplaceable(String identifier, String separator, String from, UserPlaceholderCallback callback) {
        super(identifier + separator + from, callback);
        this.identifier = identifier;
    }

    /**
     * Creates a literal user replaceable using the identifier and full key derived from a
     * {@link RATExpansion}.
     *
     * @param expansion the expansion whose builder supplies the identifier and key prefix
     * @param from      the literal placeholder key suffix
     * @param callback  the user-aware callback invoked when the placeholder is matched
     */
    public IdentifiedUserReplaceable(RATExpansion expansion, String from, UserPlaceholderCallback callback) {
        super(RATRegistry.getLiteralWithExpansion(from, expansion), callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }

    /**
     * Creates a regex-based user replaceable scoped to the given identifier.
     *
     * <p>The identifier portion is escaped to a literal, then concatenated with the
     * raw {@code regex} pattern.</p>
     *
     * @param identifier the namespace identifier prepended to the pattern
     * @param separator  the string placed between the identifier and the regex
     * @param regex      the regex pattern for the variable portion of the placeholder
     * @param groups     the number of capture groups expected in {@code regex}
     * @param callback   the user-aware callback invoked when the placeholder is matched
     */
    public IdentifiedUserReplaceable(String identifier, String separator, String regex, int groups, UserPlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(identifier + separator) + regex, groups, callback);
        this.identifier = identifier;
    }

    /**
     * Creates a regex-based user replaceable using the identifier and pattern derived
     * from a {@link RATExpansion}.
     *
     * @param expansion the expansion whose builder supplies the identifier and key prefix
     * @param regex     the regex pattern for the variable portion of the placeholder
     * @param groups    the number of capture groups expected in {@code regex}
     * @param callback  the user-aware callback invoked when the placeholder is matched
     */
    public IdentifiedUserReplaceable(RATExpansion expansion, String regex, int groups, UserPlaceholderCallback callback) {
        super(RATRegistry.getRegexWithExpansion(regex, expansion), groups, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }
}
