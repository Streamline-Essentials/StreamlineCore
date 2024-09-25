package singularity.placeholders.replaceables;

import singularity.data.console.CosmicSender;
import tv.quaint.objects.AtomicString;
import singularity.placeholders.callbacks.CallbackString;
import singularity.placeholders.callbacks.UserPlaceholderCallback;

public class UserReplaceable extends AbstractReplaceable<UserPlaceholderCallback> {
    public UserReplaceable(String from, UserPlaceholderCallback callback) {
        super(from, callback);
    }

    public UserReplaceable(String from, int groups, UserPlaceholderCallback callback) {
        super(from, groups, callback);
    }

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
