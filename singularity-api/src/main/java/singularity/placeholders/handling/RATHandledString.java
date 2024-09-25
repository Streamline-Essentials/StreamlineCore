package singularity.placeholders.handling;

import tv.quaint.thebase.lib.re2j.Matcher;
import lombok.Getter;
import tv.quaint.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RATHandledString {
    private final String regex;
    private final int groups;

    public RATHandledString(String regex, int groups) {
        this.regex = regex;
        this.groups = groups;
    }

    public boolean check(String input) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), input);
        return matcher.find();
    }

    public List<String> regexMatches(String input) {
        List<String> r = new ArrayList<>();
        Matcher matcher = MatcherUtils.matcherBuilder("(" + getRegex() + ")", input);
        List<String[]> stringArrays = MatcherUtils.getGroups(matcher, getGroups() + 1);

        for (String[] stringArray : stringArrays) {
            r.add(stringArray[0]);
        }

        return r;
    }

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

    public int count(String input) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), input);

        int i = 0;
        while (matcher.find()) {
            i++;
        }
        return i;
    }
}
