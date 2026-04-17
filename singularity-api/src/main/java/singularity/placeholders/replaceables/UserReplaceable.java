package singularity.placeholders.replaceables;

import gg.drak.thebase.objects.AtomicString;
import singularity.data.console.CosmicSender;
import singularity.placeholders.callbacks.CallbackString;
import singularity.placeholders.callbacks.UserPlaceholderCallback;

/**
 * A placeholder replaceable that resolves its value relative to a specific
 * {@link CosmicSender}.
 *
 * <p>Extends {@link AbstractReplaceable} with a {@link UserPlaceholderCallback} so
 * that the replacement logic has access to the sender (player or console) whose
 * data should populate the placeholder text.</p>
 */
public class UserReplaceable extends AbstractReplaceable<UserPlaceholderCallback> {

    /**
     * Creates a literal user replaceable.
     *
     * @param from     the literal placeholder key to match
     * @param callback the user-aware callback invoked when the placeholder is matched
     */
    public UserReplaceable(String from, UserPlaceholderCallback callback) {
        super(from, callback);
    }

    /**
     * Creates a regex-based user replaceable.
     *
     * @param from     the regex pattern used to match the placeholder
     * @param groups   the number of capture groups in the pattern
     * @param callback the user-aware callback invoked when the placeholder is matched
     */
    public UserReplaceable(String from, int groups, UserPlaceholderCallback callback) {
        super(from, groups, callback);
    }

    /**
     * Applies this replaceable to {@code string} in the context of the given
     * {@link CosmicSender}, replacing all matched occurrences with the value
     * returned by the callback.
     *
     * <p>If {@code user} is {@code null} or this replaceable is not
     * {@linkplain AbstractReplaceable#isReplaceWorthy() replace-worthy}, the original
     * string is returned unchanged.</p>
     *
     * @param string the input string that may contain placeholder tokens
     * @param user   the sender whose data is used to resolve the replacement value,
     *               or {@code null} to skip replacement
     * @return the string with all matching placeholder tokens replaced, or the
     *         original string if replacement was not performed
     */
    public String fetchAs(String string, CosmicSender user) {
        if (user == null) return string;
        if (! isReplaceWorthy()) return string;

        addTimesReplaced(getHandledString().count(string));
        AtomicString atomicString = new AtomicString(string);
        getHandledString().regexMatches(string).forEach((s) -> {
            if (getCallback() == null) return;

            atomicString.set(atomicString.get().replace(s, getCallback().apply(new CallbackString(s, getHandledString()), user)));
        });
        return atomicString.get();
    }
}
