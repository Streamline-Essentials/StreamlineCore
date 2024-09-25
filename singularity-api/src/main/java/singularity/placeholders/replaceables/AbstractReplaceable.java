package singularity.placeholders.replaceables;

import lombok.Getter;
import lombok.Setter;
import singularity.placeholders.RATRegistry;
import singularity.placeholders.callbacks.RATCallback;
import singularity.placeholders.handling.RATHandledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tv.quaint.utils.MatcherUtils;

@Setter
@Getter
public abstract class AbstractReplaceable<C extends RATCallback> implements Comparable<AbstractReplaceable<?>> {
    private RATHandledString handledString;
    @Nullable
    private C callback;
    private int timesReplaced;

    public AbstractReplaceable(String string, int groups, @Nullable C callback) {
        this.handledString = new RATHandledString(string, groups);
        this.callback = callback;
        timesReplaced = 0;
    }

    public AbstractReplaceable(String from, @Nullable C callback) {
        this(MatcherUtils.makeLiteral(from), 0, callback);
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
