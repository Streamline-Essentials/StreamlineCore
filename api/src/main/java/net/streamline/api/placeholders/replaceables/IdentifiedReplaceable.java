package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import net.streamline.api.placeholders.callbacks.PlaceholderCallback;
import net.streamline.api.placeholders.expansions.RATExpansion;
import tv.quaint.utils.MatcherUtils;

public class IdentifiedReplaceable extends GenericReplaceable {
    @Getter
    private final String identifier;

    public IdentifiedReplaceable(String identifier, String separator, String from, PlaceholderCallback callback) {
        super(identifier + separator + from, callback);
        this.identifier = identifier;
    }

    public IdentifiedReplaceable(RATExpansion expansion, String separator, String from, PlaceholderCallback callback) {
        super(expansion.getBuilder().getIdentifier() + separator + from, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }

    public IdentifiedReplaceable(String identifier, String separator, String regex, int groups, PlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(identifier + separator) + regex, groups, callback);
        this.identifier = identifier;
    }

    public IdentifiedReplaceable(RATExpansion expansion, String separator, String regex, int groups, PlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(expansion.getBuilder().getBoundingPrefix() +
                expansion.getBuilder().getIdentifier() + separator) + regex + MatcherUtils.makeLiteral(expansion.getBuilder().getBoundingSuffix()), groups, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }
}
