package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import net.streamline.api.placeholders.RATRegistry;
import net.streamline.api.placeholders.callbacks.PlaceholderCallback;
import net.streamline.api.placeholders.expansions.RATExpansion;
import tv.quaint.utils.MatcherUtils;

@Getter
public class IdentifiedReplaceable extends GenericReplaceable {
    private final String identifier;

    public IdentifiedReplaceable(String identifier, String separator, String from, PlaceholderCallback callback) {
        super(identifier + separator + from, callback);
        this.identifier = identifier;
    }

    public IdentifiedReplaceable(RATExpansion expansion, String from, PlaceholderCallback callback) {
        super(RATRegistry.getLiteralWithExpansion(from, expansion), callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }

    public IdentifiedReplaceable(String identifier, String separator, String regex, int groups, PlaceholderCallback callback) {
        super(MatcherUtils.makeLiteral(identifier + separator) + regex, groups, callback);
        this.identifier = identifier;
    }

    public IdentifiedReplaceable(RATExpansion expansion, String regex, int groups, PlaceholderCallback callback) {
        super(RATRegistry.getRegexWithExpansion(regex, expansion), groups, callback);
        this.identifier = expansion.getBuilder().getIdentifier();
    }
}
