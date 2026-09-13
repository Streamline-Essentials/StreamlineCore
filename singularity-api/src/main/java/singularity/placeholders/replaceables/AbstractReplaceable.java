package singularity.placeholders.replaceables;

import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.placeholders.RATRegistry;
import singularity.placeholders.callbacks.RATCallback;
import singularity.placeholders.handling.RATHandledString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base for all RAT (Replace-And-Transform) placeholder replaceables.
 *
 * <p>A replaceable pairs a {@link RATHandledString} (the regex pattern and
 * group count) with a typed {@link RATCallback} implementation that produces
 * the replacement text. Subclasses specialise the callback type and implement
 * the actual {@code fetch} logic.
 *
 * <p>Instances are naturally ordered by their regex pattern string and may be
 * self-registered with / deregistered from {@link RATRegistry}.
 *
 * @param <C> the specific {@link RATCallback} type used by this replaceable
 */
@Setter
@Getter
public abstract class AbstractReplaceable<C extends RATCallback> implements Comparable<AbstractReplaceable<?>> {

    /**
     * The handled-string descriptor holding the compiled regex and group count
     * used to detect this placeholder in input strings.
     */
    private RATHandledString handledString;

    /**
     * The callback invoked to produce the replacement text, or {@code null} if
     * no callback has been set (in which case replacement is skipped).
     */
    @Nullable
    private C callback;

    /**
     * Running count of how many times this replaceable has produced a replacement.
     * Starts at {@code 0} and is incremented by the {@code fetch} call on each match.
     */
    private int timesReplaced;

    /**
     * Constructs an {@code AbstractReplaceable} with a regex pattern, group count,
     * and callback.
     *
     * @param string   the regex pattern string that identifies this placeholder
     * @param groups   the number of capture groups in the pattern
     * @param callback the callback to invoke on each match, or {@code null} to
     *                 disable replacement
     */
    public AbstractReplaceable(String string, int groups, @Nullable C callback) {
        this.handledString = new RATHandledString(string, groups);
        this.callback = callback;
        timesReplaced = 0;
    }

    /**
     * Constructs an {@code AbstractReplaceable} that matches a literal string.
     *
     * <p>The supplied {@code from} value is escaped via
     * {@link MatcherUtils#makeLiteral(String)} before being stored as the
     * regex, and the group count is set to {@code 0}.
     *
     * @param from     the literal string to match; {@code null} is permitted but
     *                 will produce a pattern that never matches
     * @param callback the callback to invoke on each match, or {@code null} to
     *                 disable replacement
     */
    public AbstractReplaceable(String from, @Nullable C callback) {
        this(MatcherUtils.makeLiteral(from), 0, callback);
    }

    /**
     * Returns {@code true} if this replaceable has a non-null callback and is
     * therefore capable of producing replacements.
     *
     * @return {@code true} when {@link #callback} is not {@code null}
     */
    public boolean isReplaceWorthy() {
        return getCallback() != null;
    }

    /**
     * Returns {@code true} if this replaceable has been triggered at least once
     * (i.e. {@link #timesReplaced} is greater than zero).
     *
     * @return {@code true} if at least one replacement has occurred
     */
    public boolean hasBeenTriggered() {
        return timesReplaced > 0;
    }

    /**
     * Increments the replacement counter by the given amount.
     *
     * @param times the number of replacements to add
     */
    public void addTimesReplaced(int times) {
        timesReplaced += times;
    }

    /**
     * Increments the replacement counter by one.
     */
    public void addTimesReplaced() {
        addTimesReplaced(1);
    }

    /**
     * Resets the replacement counter to zero.
     */
    public void resetTimesReplaced() {
        timesReplaced = 0;
    }

    /**
     * Decrements the replacement counter by the given amount.
     *
     * @param times the number to subtract from the counter
     */
    public void removeTimesReplaced(int times) {
        timesReplaced -= times;
    }

    /**
     * Decrements the replacement counter by one.
     */
    public void removeTimesReplaced() {
        removeTimesReplaced(1);
    }

    /**
     * Registers this replaceable with {@link RATRegistry} so that it is
     * considered during placeholder resolution.
     */
    public void register() {
        RATRegistry.register(this);
    }

    /**
     * Removes this replaceable from {@link RATRegistry}, preventing it from
     * being applied during future placeholder resolutions.
     */
    public void unregister() {
        RATRegistry.unregister(this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ordering is determined lexicographically by the regex pattern string.
     * {@code null} patterns sort before non-null ones.
     *
     * @param o the other replaceable to compare against; must not be {@code null}
     * @return a negative integer, zero, or a positive integer as this replaceable's
     *         regex is less than, equal to, or greater than {@code o}'s regex
     */
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
