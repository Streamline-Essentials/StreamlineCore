package net.streamline.api.placeholder;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class PlaceholderUtils {
    public static Matcher setupMatcher(String regex, String from) {
        Pattern search = Pattern.compile(regex);

        return search.matcher(from);
    }

    public static PlaceholderValue getParsed(List<String> found, int i) {
        String matched = found.get(i);
        if (! (matched.startsWith("%") && matched.endsWith("%"))) return new PlaceholderValue();
        if (i + 1 >= found.size()) return new PlaceholderValue();
        if (i + 2 >= found.size()) return new PlaceholderValue();
        String identifier = found.get(i + 1);
        if (identifier == null) return new PlaceholderValue();
        String params = found.get(i + 2);
        if (params == null) return new PlaceholderValue();
        return new PlaceholderValue(matched, identifier, params);
    }

    public static List<PlaceholderValue> getParsed(List<String> found) {
        List<PlaceholderValue> placeholderValues = new ArrayList<>();

        int rotator = 0;
        boolean skipToNext = false;
        for (String string : found) {
            rotator ++;

            PlaceholderValue pv = new PlaceholderValue();
            if (rotator == 1 && placeholderValues.size() > 0) pv = placeholderValues.get(placeholderValues.size() - 1);

            switch (rotator) {
                case 1 -> {
                    skipToNext = false;
                    if (! (string.startsWith("%") && string.endsWith("%"))) {
                        skipToNext = true;
                        continue;
                    }
                    pv = pv.setUnparsed(string);
                }
                case 2 -> {
                    if (skipToNext) continue;
                    pv = pv.setIdentifier(string);
                }
                case 3 -> {
                    if (skipToNext) continue;
                    pv = pv.setParams(string);
                }
            }

            pv = pv.updateEmptiness();
            placeholderValues.add(pv);

            if (rotator == 3) rotator = 0;
        }

        return placeholderValues;
    }

//    public static List<PlaceholderValue> getParsed(List<PlaceholderValue> found) {
//        List<PlaceholderValue> placeholderValues = new ArrayList<>();
//
//        for (PlaceholderValue value : found) {
//
//        }
//
//        return placeholderValues;
//    }

    public static List<PlaceholderValue> getMatched(Matcher matcher) {
        List<PlaceholderValue> found = new ArrayList<>();

        while (matcher.find()) {
            String unparsed = matcher.group(1);
            String identifier = matcher.group(2);
            String params = matcher.group(3);
            if (unparsed == null || identifier == null || params == null) continue;
            PlaceholderValue pv = new PlaceholderValue();
            pv = pv.setUnparsed(unparsed);
            pv = pv.setIdentifier(identifier);
            pv = pv.setParams(params);
            pv = pv.updateEmptiness();
            found.add(pv);
//            MessagingUtils.logInfo("PlaceholderUtils#getMatched : found = " + pv.toString());
        }

        return found;
    }

    public static RATResult parseCustomPlaceholder(String key, String value, String from) {
        int count = 0;

        boolean isDone = false;
        String temp = from;
        while (! isDone) {
            from = from.replaceFirst(key, value);
            if (from.equals(temp)) {
                isDone = true;
                continue;
            }
            temp = from;
            count ++;
        }
        return new RATResult(from, count);
    }

    public static RATResult parsePlaceholder(RATExpansion expansion, StreamlineUser on, String from) {
        int replaced = 0;
        try {
            Matcher matcher = setupMatcher("([%](" + expansion.identifier + ")[_](.*?)[%])", from);
            List<PlaceholderValue> pvs = getMatched(matcher);

            TreeMap<String, String> toReplace = new TreeMap<>();

            for (PlaceholderValue pv : pvs) {
                if (pv.isEmpty) continue;
                String parsed = expansion.doRequest(on, pv.params);
                if (parsed == null) continue;
                pv = pv.setParsed(parsed);
                toReplace.put(pv.unparsed, pv.parsed);
            }
            for (String match : toReplace.keySet()) {
                from = from.replace(match, toReplace.get(match));
                replaced ++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new RATResult(from, replaced);
    }

    public static String parsePlaceholderJustLogic(RATExpansion expansion, String from) {
        try {
            Matcher matcher = setupMatcher("([%](" + expansion.identifier + ")[_](.*?)[%])", from);
            List<PlaceholderValue> pvs = getMatched(matcher);

            TreeMap<String, String> toReplace = new TreeMap<>();

            for (PlaceholderValue pv : pvs) {
                if (pv.isEmpty) continue;
                String parsed = expansion.doLogic(pv.params);
                if (parsed == null) continue;
                pv = pv.setParsed(parsed);
                toReplace.put(pv.unparsed, pv.parsed);
            }

            for (String match : toReplace.keySet()) {
                from = from.replace(match, toReplace.get(match));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return from;
    }
}
