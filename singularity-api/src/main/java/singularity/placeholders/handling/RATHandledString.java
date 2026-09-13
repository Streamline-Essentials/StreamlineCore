package singularity.placeholders.handling;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a compiled regex pattern and its expected capture-group count,
 * providing utility methods for checking, enumerating, and counting matches
 * within arbitrary input strings.
 *
 * <p>Instances are immutable once constructed. They are created by
 * {@link singularity.placeholders.replaceables.AbstractReplaceable} to describe
 * the pattern that triggers a particular placeholder replacement.
 */
@Getter
public class RATHandledString {

    /**
     * The regex pattern string used to detect placeholder occurrences.
     */
    private final String regex;

    /**
     * The number of capture groups defined in {@link #regex}.
     * Used to allocate correctly-sized group arrays when iterating matches.
     */
    private final int groups;

    /**
     * Constructs a new {@code RATHandledString}.
     *
     * @param regex  the regex pattern to match against input strings
     * @param groups the number of capture groups in the pattern
     */
    public RATHandledString(String regex, int groups) {
        this.regex = regex;
        this.groups = groups;
    }

    /**
     * Returns {@code true} if the given input string contains at least one
     * match of {@link #regex}.
     *
     * @param input the string to test
     * @return {@code true} if the pattern matches anywhere in {@code input}
     */
    public boolean check(String input) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), input);
        return matcher.find();
    }

    /**
     * Returns all full-pattern matches (group 0) found in the input string.
     *
     * <p>The regex is wrapped in an additional capturing group before matching
     * so that the full match text is always captured in group 1 of the
     * underlying {@link MatcherUtils#getGroups} call, which is then extracted
     * as group 0 of the returned arrays.
     *
     * @param input the string to search
     * @return a list of matched substrings; empty if there are no matches
     */
    public List<String> regexMatches(String input) {
        List<String> r = new ArrayList<>();
        Matcher matcher = MatcherUtils.matcherBuilder("(" + getRegex() + ")", input);
        List<String[]> stringArrays = MatcherUtils.getGroups(matcher, getGroups() + 1);

        for (String[] stringArray : stringArrays) {
            r.add(stringArray[0]);
        }

        return r;
    }

    /**
     * Returns all matches for the specified capture group found in the input string.
     *
     * <p>The {@code group} parameter is 1-based. Values below 1 are clamped to 1;
     * values above the total group count are clamped to {@link #groups}.
     *
     * @param input the string to search
     * @param group the 1-based capture-group index whose content should be collected
     * @return a list of captured strings for the given group across all matches;
     *         empty if there are no matches
     */
    public List<String> getRegexMatchesForGroup(String input, int group) {
        List<String> r = new ArrayList<>();
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), input);
        List<String[]> stringArrays = MatcherUtils.getGroups(matcher, getGroups());

        if (group > getGroups()) {
            group = getGroups();
        }
        group -= 1;
        if (group < 0) {
            group = 0;
        }

        for (String[] stringArray : stringArrays) {
            r.add(stringArray[group]);
        }

        return r;
    }

    /**
     * Counts the total number of non-overlapping matches of {@link #regex}
     * within the given input string.
     *
     * @param input the string to search
     * @return the number of matches found; {@code 0} if the pattern does not match
     */
    public int count(String input) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), input);

        int i = 0;
        while (matcher.find()) {
            i++;
        }
        return i;
    }
}
