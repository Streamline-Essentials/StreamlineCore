package singularity.placeholders.replaceables;

import gg.drak.thebase.objects.AtomicString;
import singularity.placeholders.callbacks.CallbackString;
import singularity.placeholders.callbacks.PlaceholderCallback;
import org.jetbrains.annotations.Nullable;

/**
 * A context-free placeholder replaceable that resolves against no specific user.
 *
 * <p>{@code GenericReplaceable} pairs a regex pattern (via
 * {@link AbstractReplaceable}) with a {@link PlaceholderCallback} that receives
 * a {@link CallbackString} for each match and returns the replacement text.
 * Substitutions are applied to all non-overlapping matches in the input string
 * in a single {@link #fetch(String)} call.
 *
 * <p>Example:
 * <pre>{@code
 * GenericReplaceable r = new GenericReplaceable(
 *         "%server_name%",
 *         cs -> Singularity.getInstance().getName());
 * r.register();
 * }</pre>
 */
public class GenericReplaceable extends AbstractReplaceable<PlaceholderCallback> {

    /**
     * Constructs a {@code GenericReplaceable} with a regex pattern, an explicit
     * group count, and a callback.
     *
     * @param from     the regex pattern that identifies this placeholder;
     *                 {@code null} produces a pattern that never matches
     * @param groups   the number of capture groups in the pattern
     * @param callback the callback invoked for each match, or {@code null} to
     *                 disable replacement
     */
    public GenericReplaceable(@Nullable String from, int groups, @Nullable PlaceholderCallback callback) {
        super(from, groups, callback);
    }

    /**
     * Constructs a {@code GenericReplaceable} from a literal string with no
     * capture groups.
     *
     * <p>The string is escaped via
     * {@link gg.drak.thebase.utils.MatcherUtils#makeLiteral(String)} before
     * being stored as the pattern, so no regex metacharacters are interpreted.
     *
     * @param from     the literal string to match; {@code null} is permitted
     * @param callback the callback invoked for each match, or {@code null} to
     *                 disable replacement
     */
    public GenericReplaceable(@Nullable String from, @Nullable PlaceholderCallback callback) {
        super(from, callback);
    }

    /**
     * Applies the callback to every occurrence of this replaceable's pattern
     * in the given string and returns the result.
     *
     * <p>If this replaceable has no callback ({@link #isReplaceWorthy()} returns
     * {@code false}), the original string is returned unchanged. Otherwise,
     * the replacement counter is incremented by the match count before
     * substitutions are applied.
     *
     * @param string the input string to process
     * @return the input string with all matching placeholder tokens replaced by
     *         the values returned from the callback
     */
    public String fetch(String string) {
        if (! isReplaceWorthy()) return string;

        addTimesReplaced(getHandledString().count(string));
        AtomicString atomicString = new AtomicString(string);
        getHandledString().regexMatches(string).forEach((s) -> {
            if (getCallback() == null) return;

            atomicString.set(atomicString.get().replace(s, getCallback().apply(new CallbackString(s, getHandledString()))));
        });
        return atomicString.get();
    }
}
