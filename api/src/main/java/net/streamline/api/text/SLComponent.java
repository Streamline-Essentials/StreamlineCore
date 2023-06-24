package net.streamline.api.text;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.utils.MatcherUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

public class SLComponent implements Comparable<SLComponent> {
    public static final String COMPONENT_STARTER = "{{-";
    public static final String COMPONENT_FINISHER = "-}}";
    public static final String COMPONENT_REGEX = "(([{][{]-)(.*?)(-[}][}]))";
    public static final String COLOR_PREFIX = "§";

    @Getter
    private final String realRaw;
    @Getter
    private final String realMain;

    @Getter @Setter
    private String raw;
    @Getter @Setter
    private int substringStart;
    @Getter @Setter
    private String main;

    public SLComponent(String raw, int substringStart, String main) {
        this.realRaw = raw;
        this.realMain = main;

        this.raw = raw;
        this.substringStart = substringStart;
        this.main = main;
    }

    public SLComponent(String raw, String main) {
        this(raw, 0, main);
    }

    public int rawLength() {
        return raw.length();
    }

    public int realRawLength() {
        return realRaw.length();
    }

    public int end() {
        return substringStart + rawLength();
    }

    public int realEnd() {
        return substringStart + realRawLength();
    }

    public static ConcurrentSkipListMap<Integer, SLComponent> extract(String raw) {
        ConcurrentSkipListMap<Integer, SLComponent> r = new ConcurrentSkipListMap<>();

        String clone = raw;

        Matcher matcher = MatcherUtils.matcherBuilder(COMPONENT_REGEX, clone);
        List<String[]> matches = MatcherUtils.getGroups(matcher, 4);

        for (String[] match : matches) {
            String rawMatch = match[0];
            String main = match[2];

            int start = clone.indexOf(rawMatch);
            int end = start + rawMatch.length();

            clone = clone.replaceFirst(COMPONENT_REGEX, "");

            r.put(start, new SLComponent(rawMatch, start, main));
        }

        // Clean Up.
        r.forEach((integer, slComponent) -> {
            try {
                DataComponent component = new DataComponent(slComponent.getRaw(), slComponent.getSubstringStart(), slComponent.getMain());
                ChatComponent chatComponent = ChatComponent.transpose(component);
                r.put(integer, chatComponent);
            } catch (Exception ignored) {}
        });

        return r;
    }

    @Override
    public int compareTo(@NotNull SLComponent o) {
        // if substringStart is the same, compare raw
        if (substringStart == o.substringStart) {
            return realRaw.compareTo(o.realRaw);
        }
        // otherwise, compare substringStart
        return Integer.compare(substringStart, o.substringStart);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("SLComponent{");
        builder.append("raw=").append(raw.replace("{{-", "\\{\\{\\-").replace("-}}", "\\-\\}\\}"));
        builder.append(", substringStart=").append(substringStart);
        builder.append(", main=").append(main);
        builder.append("}");

        return builder.toString();
    }
}
