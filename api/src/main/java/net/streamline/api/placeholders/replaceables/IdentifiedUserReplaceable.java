package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import net.streamline.api.placeholders.callbacks.UserPlaceholderCallback;
import net.streamline.api.placeholders.expansions.RATExpansion;
import tv.quaint.utils.MatcherUtils;

public class IdentifiedUserReplaceable extends UserReplaceable {
    @Getter
    private final String identifier;

    public IdentifiedUserReplaceable(String identifier, String separator, String from, UserPlaceholderCallback callback) {
        super(identifier + separator + from, callback);
        this.identifier = identifier;
    }

    public IdentifiedUserReplaceable(RATExpansion expansion, String separator, String from, UserPlaceholderCallback callback) {
        super(expansion.getBuilder().getIdentifier() + separator + from, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }

    public IdentifiedUserReplaceable(String identifier, String separator, String regex, int groups, UserPlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(identifier + separator) + regex, groups, callback);
        this.identifier = identifier;
    }

    public IdentifiedUserReplaceable(RATExpansion expansion, String separator, String regex, int groups, UserPlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(expansion.getBuilder().getBoundingPrefix() +
                expansion.getBuilder().getIdentifier() + separator) + regex + MatcherUtils.makeLiteral(expansion.getBuilder().getBoundingSuffix()), groups, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }
}
