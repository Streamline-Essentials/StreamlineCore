package singularity.text;

import lombok.Getter;
import lombok.Setter;
import tv.quaint.objects.AtomicString;
import org.jetbrains.annotations.NotNull;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.utils.MatcherUtils;

import java.util.List;

@Setter
@Getter
public class HexReplacer implements Comparable<HexReplacer> {
    public static final String HEX_REGEX = "([0-9a-fA-F]{6})";
    public static final String FULL_REGEX = "((%starter%)" + HEX_REGEX + "(%ender%))";

    private String starter;
    private String ender;

    private String setTo;

    public HexReplacer(String starter, String ender, String setTo) {
        this.starter = starter;
        this.ender = ender;
        this.setTo = setTo;
    }

    public HexReplacer() {
        this("{#", "}", "<#%hex%>");
    }

    public String getWith(String hex) {
        return getStarter() + hex + getEnder();
    }

    public String getRegex() {
        return FULL_REGEX
                .replace("%starter%", MatcherUtils.makeLiteral(getStarter()))
                .replace("%ender%", MatcherUtils.makeLiteral(getEnder()))
                ;
    }

    public List<String[]> scan(String from) {
        Matcher matcher = MatcherUtils.matcherBuilder(getRegex(), from);
        return MatcherUtils.getGroups(matcher, 4);
    }

    /**
     * Replaces all found {@link #getRegex()} with {@link #getSetTo()} where {@link #getSetTo()}'s "%hex%" is replaced with the found hex.
     * @param text The text to replace.
     */
    public AtomicString replace(AtomicString text) {
        scan(text.get()).forEach(group -> {
            String hex = group[2];
            String with = getSetTo().replace("%hex%", hex);
            text.set(text.get().replace(group[1], with));
        });

        return text;
    }

    public String getIdentifiably() {
        return getStarter() + "123456" + getEnder();
    }

    @Override
    public int compareTo(@NotNull HexReplacer o) {
        return getIdentifiably().compareTo(o.getIdentifiably());
    }
}
