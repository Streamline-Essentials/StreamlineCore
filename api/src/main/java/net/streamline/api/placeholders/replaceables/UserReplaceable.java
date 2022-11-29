package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import net.streamline.api.objects.AtomicString;
import net.streamline.api.placeholders.callbacks.CallbackString;
import net.streamline.api.placeholders.callbacks.UserPlaceholderCallback;
import net.streamline.api.savables.users.StreamlineUser;

public class UserReplaceable extends AbstractReplaceable<UserPlaceholderCallback> {
    public UserReplaceable(String from, UserPlaceholderCallback callback) {
        super(from, callback);
    }

    public UserReplaceable(String from, int groups, UserPlaceholderCallback callback) {
        super(from, groups, callback);
    }

    public String fetchAs(String string, StreamlineUser user) {
        if (! isReplaceWorthy()) return string;

        addTimesReplaced(getHandledString().count(string));
        AtomicString atomicString = new AtomicString(string);
        getHandledString().isolateIn(string).forEach((s) -> {
            atomicString.set(atomicString.get().replace(s, getCallback().apply(new CallbackString(s, getHandledString()), user)));
        });
        return atomicString.get();
    }
}
