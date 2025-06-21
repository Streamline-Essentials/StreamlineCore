package singularity.data.players.meta;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.objects.Identifiable;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter @Setter
public class MetaTag<O> implements Identifiable {
    private String identifier;

    private String serializedValue;
    private MetaRetriever<O> objectReturner;

    public MetaTag(String identifier, Supplier<String> valueGetter, MetaRetriever<O> objectReturner) {
        this.identifier = identifier;
        this.serializedValue = valueGetter.get();
        this.objectReturner = objectReturner;
    }

    public MetaTag(String identifier, String value) {
        this(identifier, () -> value, (s) -> (O) s);
    }

    public MetaTag(String identifier, boolean value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Boolean.valueOf(s));
    }

    public MetaTag(String identifier, int value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Integer.valueOf(s));
    }

    public MetaTag(String identifier, long value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Long.valueOf(s));
    }

    public MetaTag(String identifier, double value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Double.valueOf(s));
    }

    public MetaTag(String identifier, float value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Float.valueOf(s));
    }

    public <T> MetaTag(String identifier, List<T> value, MetaRetriever<T> objectReturner) {
        this(identifier, () -> {
            StringBuilder builder = new StringBuilder();
            for (T t : value) {
                builder.append("!!!").append(t).append(";;");
            }
            return builder.toString();
        }, (s) -> {
            List<T> r = new ArrayList<>();

            Matcher matcher = MatcherUtils.matcherBuilder("(!!!)(.*?)(;;)", s);
            List<String[]> matches = MatcherUtils.getGroups(matcher, 3);
            for (String[] match : matches) {
                try {
                    T t = objectReturner.apply(match[1]);
                    r.add(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return (O) r;
        });
    }
}
