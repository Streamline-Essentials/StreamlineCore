package net.streamline.api.placeholders.replaceables;

import net.streamline.api.objects.AtomicString;
import net.streamline.api.placeholders.callbacks.CallbackString;
import net.streamline.api.placeholders.callbacks.PlaceholderCallback;
import org.jetbrains.annotations.Nullable;

public class GenericReplaceable extends AbstractReplaceable<PlaceholderCallback> {
    public GenericReplaceable(@Nullable String from, int groups, @Nullable PlaceholderCallback callback) {
        super(from, groups, callback);
    }

    public GenericReplaceable(@Nullable String from, @Nullable PlaceholderCallback callback) {
        super(from, callback);
    }

    public String fetch(String string) {
        if (! isReplaceWorthy()) return string;

        addTimesReplaced(getHandledString().count(string));
        AtomicString atomicString = new AtomicString(string);
        getHandledString().isolateIn(string).forEach((s) -> {
            atomicString.set(atomicString.get().replace(s, getCallback().apply(new CallbackString(s, getHandledString()))));
        });
        return atomicString.get();
    }
}
