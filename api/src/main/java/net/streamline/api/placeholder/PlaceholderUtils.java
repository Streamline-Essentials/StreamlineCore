package net.streamline.api.placeholder;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MatcherUtils;
import net.streamline.api.utils.MathUtils;
import net.streamline.api.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlaceholderUtils {
    public static boolean containsReplacement(ConcurrentSkipListSet<RATReplacement> replacements, String totalToCheck) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        replacements.forEach(replacement -> {
            if (replacement.getTotal().equals(totalToCheck)) atomicBoolean.set(true);
        });

        return atomicBoolean.get();
    }

    public static ConcurrentSkipListSet<RATReplacement> getReplacements(StreamlineUser user, String from) {
        ConcurrentSkipListSet<RATReplacement> replacements = new ConcurrentSkipListSet<>();

        SLAPI.getRatAPI().getLoadedExpansions().forEach((integerDatedNumber, expansion) -> {
            replacements.addAll(getReplacements(user, expansion, from));
        });

        return replacements;
    }

    public static ConcurrentSkipListSet<RATReplacement> getReplacements(StreamlineUser user, RATExpansion expansion, String from) {
        Matcher matcher = MatcherUtils.matcherBuilder("([%](" + expansion.getIdentifier() + ")[_](.*?)[%])", from);

        ConcurrentSkipListSet<RATReplacement> found = new ConcurrentSkipListSet<>();

        while (matcher.find()) {
            String total = matcher.group(1);
            String identifier = matcher.group(2);
            String params = matcher.group(3);
            if (total == null || identifier == null || params == null) continue;
            if (expansion.containsCached(user, params)){
                found.add(new RATReplacement(total, identifier, params, expansion.getCached(user, params)));
                continue;
            }
            String value = expansion.doRequest(user, params);
            expansion.cache(user, params, value);
            if (value == null) continue;
            RATReplacement replacement = new RATReplacement(total, identifier, params, value);
            if (containsReplacement(found, replacement.getTotal())) continue;
            found.add(replacement);
        }

        return found;
    }

    public static ConcurrentSkipListSet<RATReplacement> getReplacements(String from) {
        ConcurrentSkipListSet<RATReplacement> replacements = new ConcurrentSkipListSet<>();

        SLAPI.getRatAPI().getLoadedExpansions().forEach((integerDatedNumber, expansion) -> {
            replacements.addAll(getReplacements(expansion, from));
        });

        return replacements;
    }

    public static ConcurrentSkipListSet<RATReplacement> getReplacements(RATExpansion expansion, String from) {
        Matcher matcher = MatcherUtils.matcherBuilder("([%](" + expansion.getIdentifier() + ")[_](.*?)[%])", from);

        ConcurrentSkipListSet<RATReplacement> found = new ConcurrentSkipListSet<>();

        while (matcher.find()) {
            String total = matcher.group(1);
            String identifier = matcher.group(2);
            String params = matcher.group(3);
            if (total == null || identifier == null || params == null) continue;
            if (expansion.containsCached(UserUtils.getConsole(), params)){
                found.add(new RATReplacement(total, identifier, params, expansion.getCached(UserUtils.getConsole(), params)));
                continue;
            }
            String value = expansion.doLogic(params);
            expansion.cache(UserUtils.getConsole(), params, value);
            if (value == null) continue;
            RATReplacement replacement = new RATReplacement(total, identifier, params, value);
            if (containsReplacement(found, replacement.getTotal())) continue;
            found.add(replacement);
        }

        return found;
    }

    public static ConcurrentSkipListSet<RATReplacement> getReplacementsCustom(String from) {
        ConcurrentSkipListSet<RATReplacement> replacements = new ConcurrentSkipListSet<>();

        SLAPI.getRatAPI().getCustomPlaceholders().forEach((integerDatedNumber, customPlaceholder) -> {
            replacements.addAll(getReplacements(customPlaceholder, from));
        });

        return replacements;
    }

    public static ConcurrentSkipListSet<RATReplacement> getReplacements(CustomPlaceholder customPlaceholder, String from) {
        ConcurrentSkipListSet<RATReplacement> found = new ConcurrentSkipListSet<>();
        Matcher matcher = MatcherUtils.matcherBuilder("(" + customPlaceholder.getKey() + ")", from);

        while (matcher.find()) {
            String total = matcher.group(1);
            if (total == null) continue;
            RATReplacement replacement = new RATReplacement(total, total, total, customPlaceholder.getValue());
            if (replacement.getReplacement() == null) continue;
            if (containsReplacement(found, replacement.getTotal())) continue;
            found.add(replacement);
        }

        return found;
    }

    public static ConcurrentSkipListSet<RATResult> getResults(ConcurrentSkipListSet<RATReplacement> replacements) {
        ConcurrentSkipListSet<RATResult> r = new ConcurrentSkipListSet<>();

        replacements.forEach(replacement -> r.add(new RATResult(replacement)));

        return r;
    }

    public static ConcurrentSkipListSet<RATResult> getAllPlaceholderResults(StreamlineUser user, String from) {
        ConcurrentSkipListSet<RATResult> r = new ConcurrentSkipListSet<>();

        r.addAll(getResults(getReplacements(user, from)));
//        r.addAll(getResults(getReplacements(from))); // Don't think we need this?
        r.addAll(getResults(getReplacementsCustom(from)));

        return r;
    }

    public static ConcurrentSkipListSet<RATResult> getLogicPlaceholderResults(String from) {
        ConcurrentSkipListSet<RATResult> r = new ConcurrentSkipListSet<>();

        r.addAll(getResults(getReplacements(from)));

        return r;
    }

    public static ConcurrentSkipListSet<RATResult> getAllLogicalPlaceholderResults(String from) {
        ConcurrentSkipListSet<RATResult> r = new ConcurrentSkipListSet<>();

        r.addAll(getResults(getReplacements(from)));
        r.addAll(getResults(getReplacementsCustom(from)));

        return r;
    }

    public static ConcurrentSkipListSet<RATResult> getCustomPlaceholderResults(String from) {
        ConcurrentSkipListSet<RATResult> r = new ConcurrentSkipListSet<>();

        r.addAll(getResults(getReplacementsCustom(from)));

        return r;
    }

    public static ConcurrentSkipListSet<RATResult> getUserPlaceholderResults(StreamlineUser user, String from) {
        ConcurrentSkipListSet<RATResult> r = new ConcurrentSkipListSet<>();

        r.addAll(getResults(getReplacements(user, from)));

        return r;
    }
}
