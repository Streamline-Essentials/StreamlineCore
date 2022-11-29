package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.placeholders.RATRegistry;
import net.streamline.api.placeholders.callbacks.RATCallback;
import net.streamline.api.placeholders.handling.RATHandledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tv.quaint.utils.MatcherUtils;

public abstract class AbstractReplaceable<C extends RATCallback> implements Comparable<AbstractReplaceable<?>> {
    @Getter @Setter
    private RATHandledString handledString;
    @Getter @Setter @Nullable
    private C callback;
    @Getter @Setter
    private int timesReplaced;

    public AbstractReplaceable(String string, int groups, @Nullable C callback) {
        this.handledString = new RATHandledString(string, groups);
        this.callback = callback;
        timesReplaced = 0;
    }

    public AbstractReplaceable(String from, @Nullable C callback) {
        this(MatcherUtils.makeLiteral(from), 1, callback);
    }

    public boolean isReplaceWorthy() {
        return getCallback() != null;
    }

    public boolean hasBeenTriggered() {
        return timesReplaced > 0;
    }

    public void addTimesReplaced(int times) {
        timesReplaced += times;
    }

    public void addTimesReplaced() {
        addTimesReplaced(1);
    }

    public void resetTimesReplaced() {
        timesReplaced = 0;
    }

    public void removeTimesReplaced(int times) {
        timesReplaced -= times;
    }

    public void removeTimesReplaced() {
        removeTimesReplaced(1);
    }

    public void register() {
        RATRegistry.register(this);
    }

    public void unregister() {
        RATRegistry.unregister(this);
    }

    @Override
    public int compareTo(@NotNull AbstractReplaceable<?> o) {
        String from = getHandledString().getRegex();
        String oFrom = o.getHandledString().getRegex();
        if (from == null && oFrom == null) return 0;
        if (from == null) return -1;
        if (oFrom == null) return 1;
        return CharSequence.compare(from, oFrom);
    }
}
