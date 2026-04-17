package singularity.placeholders.callbacks;

import singularity.placeholders.handling.RATHandledString;

import java.util.List;

/**
 * Wraps a raw input string together with the {@link RATHandledString} that
 * matched it, providing convenient accessor methods for extracting individual
 * regex capture groups from placeholder matches.
 *
 * <p>A {@code CallbackString} is created by the placeholder resolution
 * pipeline and passed to {@link PlaceholderCallback} / {@link UserPlaceholderCallback}
 * implementations so they can inspect the placeholder's parameters.
 */
public class CallbackString {

    /** The raw input string that was tested against the regex. */
    private final String string;

    /** The handled-string descriptor that defines the regex and group count. */
    private final RATHandledString handledString;

    /**
     * Returns the raw input string that was matched.
     *
     * @return the raw input string
     */
    public String string() {
        return string;
    }

    /**
     * Returns the {@link RATHandledString} associated with this callback.
     *
     * @return the handled-string descriptor
     */
    public RATHandledString handledString() {
        return handledString;
    }

    /**
     * Constructs a new {@code CallbackString}.
     *
     * @param string        the raw input string that was matched
     * @param handledString the handled-string descriptor used for the match
     */
    public CallbackString(String string, RATHandledString handledString) {
        this.string = string;
        this.handledString = handledString;
    }

    /**
     * Returns all regex matches found in the raw string for the given capture group.
     *
     * @param group the capture-group index (1-based)
     * @return a list of matched strings for the specified group
     */
    public List<String> simplyGet(int group) {
        return handledString.getRegexMatchesForGroup(string, group);
    }

    /**
     * Returns the match at the specified index within the given capture group.
     *
     * @param index the zero-based position in the list of matches
     * @param group the capture-group index (1-based)
     * @return the matched string at the given index and group
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public String get(int index, int group) {
        return simplyGet(group).get(index);
    }

    /**
     * Returns the match at the specified index within capture group 1.
     *
     * @param index the zero-based position in the list of matches
     * @return the matched string at the given index in group 1
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public String get(int index) {
        return get(index, 1);
    }

    /**
     * Returns the first match in capture group 1.
     *
     * <p>This is the most common accessor for simple single-match placeholders.
     *
     * @return the first matched string in group 1
     * @throws IndexOutOfBoundsException if there are no matches
     */
    public String get() {
        return get(0);
    }

    /**
     * Returns the first match in the specified capture group.
     *
     * @param group the capture-group index (1-based)
     * @return the first matched string in the given group
     * @throws IndexOutOfBoundsException if there are no matches
     */
    public String getSimpleGroup(int group) {
        return get(0, group);
    }
}
