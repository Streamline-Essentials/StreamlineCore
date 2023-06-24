package net.streamline.api.text;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.utils.MatcherUtils;
import tv.quaint.utils.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class DataPart implements Comparable<DataPart> {
    public static final String DATA_SEPARATOR = "!";
    public static final String DATA_PARTITIONER = "=";
    public static final String DATA_ENDER = ";";
    public static final String DATA_REGEX = "(" + MatcherUtils.makeLiteral(DATA_SEPARATOR) + "(.*?)" + MatcherUtils.makeLiteral(DATA_PARTITIONER) + "(.*?)" + MatcherUtils.makeLiteral(DATA_ENDER) + ")";

    @Getter @Setter
    private String raw;
    @Getter @Setter
    private String key;
    @Getter @Setter
    private String value;

    public DataPart(String raw, String key, String value) {
        this.raw = raw;
        this.key = key;
        this.value = value;
    }

    public DataPart(String key, String value) {
        this("", key, value);

        this.raw = parseNewRaw();
    }

    public String parseNewRaw() {
        return DATA_SEPARATOR + key + DATA_PARTITIONER + value + DATA_ENDER;
    }

    @Override
    public int compareTo(@NotNull DataPart o) {
        return o.getKey().compareTo(this.getKey());
    }

    public static ConcurrentSkipListSet<DataPart> fromRaw(String raw) {
        Matcher matcher = MatcherUtils.matcherBuilder(DATA_REGEX, raw);
        List<String[]> matches = MatcherUtils.getGroups(matcher, 3);

        ConcurrentSkipListSet<DataPart> r = new ConcurrentSkipListSet<>();

        for (String[] match : matches) {
            String rawMatch = match[0];
            String key = match[1];
            String value = match[2];

            r.add(new DataPart(rawMatch, key, value));
        }

        return r;
    }
}
