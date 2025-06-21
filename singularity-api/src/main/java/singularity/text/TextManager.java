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

public class TextManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<HexPolicy> hexPolicies = new ConcurrentSkipListSet<>();

    public static void registerHexPolicy(HexPolicy resulter) {
        hexPolicies.add(resulter);

        MessageUtils.logInfo("Registered HexPolicy with starter '" + resulter.getStarter() + "' and ender '" + resulter.getEnder() + "'.");
    }

    public static void registerHexPolicy(String starter, String ender) {
        registerHexPolicy(new HexPolicy(starter, ender));
    }

//    public static void registerHexPolicy(String starter, String ender, String setTo) {
//        registerHexPolicy(new HexPolicy(starter, ender, setTo));
//    }

    public static void unregisterHexPolicy(HexPolicy resulter) {
        unregisterHexPolicy(resulter.getIdentifiably());
    }

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
