package net.streamline.api.placeholders.handling;

import com.google.re2j.Matcher;
import lombok.Getter;
import tv.quaint.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.List;

public class RATHandledString {
    @Getter
    private final String regex;
    @Getter
    private final int groups;

    public RATHandledString(String regex, int groups) {
        this.regex = regex;
        this.groups = groups;
    }

    public boolean check(String input) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), input);
        return matcher.find();
    }

    public List<String> isolateIn(String input) {
        List<String> r = new ArrayList<>();
        Matcher matcher = MatcherUtils.matcherBuilder("(" + getRegex() + ")", input);
        List<String[]> stringArrays = MatcherUtils.getGroups(matcher, getGroups());

        for (String[] stringArray : stringArrays) {
            r.add(stringArray[0]);
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
