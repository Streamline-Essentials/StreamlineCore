package singularity.text;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.lib.re2j.Pattern;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Central registry for hex-colour parsing rules.
 *
 * <p>{@code TextManager} maintains a sorted set of {@link HexPolicy} objects that
 * describe delimiter pairs used to mark hex colours in user-facing text.  Additional
 * utilities extract embedded hex codes and balanced JSON sub-strings from arbitrary
 * text input.
 */
public class TextManager {

    /**
     * The set of all registered hex-colour policies, sorted by their
     * {@link HexPolicy#getIdentifiably()} value.
     */
    @Getter @Setter
    private static ConcurrentSkipListSet<HexPolicy> hexPolicies = new ConcurrentSkipListSet<>();

    /**
     * Registers a {@link HexPolicy} and logs a confirmation message.
     *
     * @param resulter the policy to register; must not be {@code null}
     */
    public static void registerHexPolicy(HexPolicy resulter) {
        hexPolicies.add(resulter);

        MessageUtils.logInfo("Registered HexPolicy with starter '" + resulter.getStarter() + "' and ender '" + resulter.getEnder() + "'.");
    }

    /**
     * Creates and registers a {@link HexPolicy} with the given delimiter strings.
     *
     * @param starter the opening delimiter
     * @param ender   the closing delimiter
     */
    public static void registerHexPolicy(String starter, String ender) {
        registerHexPolicy(new HexPolicy(starter, ender));
    }

//    public static void registerHexPolicy(String starter, String ender, String setTo) {
//        registerHexPolicy(new HexPolicy(starter, ender, setTo));
//    }

    /**
     * Removes the given {@link HexPolicy} from the registry by its identifiable string.
     *
     * @param resulter the policy to remove; must not be {@code null}
     */
    public static void unregisterHexPolicy(HexPolicy resulter) {
        unregisterHexPolicy(resulter.getIdentifiably());
    }

    /**
     * Removes every registered {@link HexPolicy} whose identifiable string matches
     * the given value.
     *
     * @param identifiably the identifiable string of the policy to remove
     */
    public static void unregisterHexPolicy(String identifiably) {
        hexPolicies.removeIf(resulter -> resulter.getIdentifiably().equals(identifiably));
    }

//    public static String replaceHex(String text) {
//        AtomicString atomicString = new AtomicString(text);
//
//        getHexPolicies().forEach(hexPolicy -> {
//            hexPolicy.replace(atomicString);
//        });
//
//        return text;
//    }

    /**
     * Extracts all six-digit hex colour codes from the input string that are enclosed
     * by the delimiters of the given {@link HexPolicy}.
     *
     * @param input     the text to scan
     * @param hexPolicy the delimiter policy defining what counts as a hex token
     * @return an ordered list of six-character hex strings found in the input
     */
    public static List<String> extractHexCodes(String input, HexPolicy hexPolicy) {
        List<String> hexCodes = new ArrayList<>();

        String regex =
                (Objects.equals(hexPolicy.getStarter(), "") || hexPolicy.getStarter() == null ? "" : MatcherUtils.makeLiteral(hexPolicy.getStarter()))
                        + "([a-fA-F0-9]{6})" +
                        (Objects.equals(hexPolicy.getEnder(), "") || hexPolicy.getEnder() == null ? "" : MatcherUtils.makeLiteral(hexPolicy.getEnder()));
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            hexCodes.add(matcher.group(1));
        }

        return hexCodes;
    }

    /**
     * Extracts balanced JSON object strings from the input text that begin immediately
     * after each occurrence of {@code startMarker}.
     *
     * <p>The extraction uses brace counting to locate the matching closing brace so
     * nested objects are handled correctly.
     *
     * @param input       the text to search
     * @param startMarker the literal string that precedes each JSON object
     * @return a list of JSON object strings (including their outer braces)
     */
    public static List<String> extractJsonStrings(String input, String startMarker) {
        List<String> jsonStrings = new ArrayList<>();
        int index = 0;

        while ((index = input.indexOf(startMarker, index)) != -1) {
            int braceCount = 0;
            int i;

            for (i = index + startMarker.length(); i < input.length(); i++) {
                char c = input.charAt(i);

                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;

                    if (braceCount == 0) {
                        break;
                    }
                }
            }

            if (braceCount == 0) {
                jsonStrings.add(input.substring(index + startMarker.length(), i + 1));
            }

            index = i + 1;
        }

        return jsonStrings;
    }
}
