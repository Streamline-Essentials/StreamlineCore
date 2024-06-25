package net.streamline.api.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleLike;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.scheduler.BaseRunnable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MessageUtils {
    public static void init() {
    }

    public static void logInfo(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleInfoDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleInfoPrefix() + line);
        }
    }

    public static void logWarning(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleWarningsDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleWarningsPrefix() + line);
        }
    }

    public static void logSevere(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleErrorsDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleErrorsPrefix() + line);
        }
    }

    public static void logDebug(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleDebugDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.sendConsoleMessage(GivenConfigs.getMainConfig().debugConsoleDebugPrefix() + line);
        }
    }

    public static void logInfo(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logInfo(stackTraceElement.toString());
        });
    }

    public static void logWarning(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logWarning(stackTraceElement.toString());
        });
    }

    public static void logSevere(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logSevere(stackTraceElement.toString());
        });
    }

    public static void logDebug(StackTraceElement[] stackTraceElements) {
        Arrays.stream(stackTraceElements).forEach(stackTraceElement -> {
            logDebug(stackTraceElement.toString());
        });
    }

    public static void logInfo(Exception e) {
        logInfo(e.getMessage());
        logInfo(e.getStackTrace());
    }

    public static void logWarning(Exception e) {
        logWarning(e.getMessage());
        logWarning(e.getStackTrace());
    }

    public static void logSevere(Exception e) {
        logSevere(e.getMessage());
        logSevere(e.getStackTrace());
    }

    public static void logDebug(Exception e) {
        logDebug(e.getMessage());
        logDebug(e.getStackTrace());
    }

    public static void logInfo(String message, Throwable throwable) {
        logInfo(message);
        logInfo(throwable.getMessage());
        logInfo(throwable.getStackTrace());
    }

    public static void logWarning(String message, Throwable throwable) {
        logWarning(message);
        logWarning(throwable.getMessage());
        logWarning(throwable.getStackTrace());
    }

    public static void logSevere(String message, Throwable throwable) {
        logSevere(message);
        logSevere(throwable.getMessage());
        logSevere(throwable.getStackTrace());
    }

    public static void logDebug(String message, Throwable throwable) {
        logDebug(message);
        logDebug(throwable.getMessage());
        logDebug(throwable.getStackTrace());
    }

    public static void logInfoWithInfo(String message, Throwable throwable) {
        logInfo(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    public static void logWarningWithInfo(String message, Throwable throwable) {
        logWarning(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    public static void logSevereWithInfo(String message, Throwable throwable) {
        logSevere(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    public static void logDebugWithInfo(String message, Throwable throwable) {
        logDebug(message + (message.endsWith(" ") ? "" : " ") + throwable.getMessage(), throwable);
    }

    public static String loggedModulePrefix(ModuleLike module) {
        return "[" + module.getIdentifier() + "] ";
    }

    public static void logInfo(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logInfo(loggedModulePrefix(module) + line);
        }
    }

    public static void logWarning(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logWarning(loggedModulePrefix(module) + line);
        }
    }

    public static void logSevere(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logSevere(loggedModulePrefix(module) + line);
        }
    }

    public static void logDebug(ModuleLike module, String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            logDebug(loggedModulePrefix(module) + line);
        }
    }

    public static void logInfo(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logInfo(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    public static void logWarning(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logWarning(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    public static void logSevere(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logSevere(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    public static void logDebug(ModuleLike module, StackTraceElement[] elements) {
        Arrays.stream(elements).forEach(stackTraceElement -> {
            logDebug(loggedModulePrefix(module) + stackTraceElement);
        });
    }

    public static void sendMessage(String to, String message) {
        StreamSender user = UserUtils.getOrCreateSender(to);

        SLAPI.getInstance().getMessenger().sendMessage(user, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        StreamSender user = UserUtils.getOrCreateSender(to);

        SLAPI.getInstance().getMessenger().sendMessage(user, replaceAllPlayerBungee(otherUUID, message));
    }

    public static String replaceAllPlayerBungee(StreamSender user, String of) {
        if (user == null) return of;

//        return SLAPI.getRatAPI().parseAllPlaceholders(user, of).completeOnTimeout(of, 77, TimeUnit.MILLISECONDS).join();

        SingleSet<String, String> key = new SingleSet<>(user.getUuid(), of);
        String cached = CACHE.getIfPresent(key);

        if (cached != null) return cached;
        else {
            String parsed = ModuleUtils.replacePlaceholders(user, of);
            CACHE.put(key, parsed);

            return parsed;
        }
    }

    public static String replaceAllPlayerBungee(String to, String of) {
        StreamSender user = UserUtils.getOrCreateSender(to);

        return replaceAllPlayerBungee(user, of);
    }

    public static Cache<SingleSet<String, String>, String> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .build();

    public static String parseOnProxy(StreamSender streamSender, String toParse) {
        StreamPlayer player;
        if (streamSender instanceof StreamPlayer) {
            player = (StreamPlayer) streamSender;
        } else {
            try {
                player = UserUtils.getLoadedPlayersSet().first();
            } catch (Exception e) {
                player = null;
            }
        }
        if (player == null) {
            streamSender.getReplacements().addReplacement(toParse, "&cNo Valid Proxy-able Player");
        }

        ReturnableMessage message = ProxyParseMessageBuilder.build(player, toParse, streamSender);

        message.registerEventCall(m -> {
            String parsed = ProxyParseMessageBuilder.parse(m);

            streamSender.getReplacements().addReplacement(toParse, parsed);
        });

        message.send();

        return streamSender.getReplacements().getReplacement(toParse, "Loading...");
    }

    public static String getListAsFormattedString(List<?> list) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i ++) {
            String tag = String.valueOf(list.get(i));

            if (i < list.size() - 1) {
                builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_BASE.get().replace("%value%", tag));
            } else {
                builder.append(MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.LISTS_LAST.get().replace("%value%", tag));
            }
        }

        return builder.toString();
    }

    public static String removeExtraDot(String string){
        String s = string.replace("..", ".");

        if (s.endsWith(".")) {
            s = s.substring(0, s.lastIndexOf('.'));
        }

        return s;
    }

    public static String resize(String text, int digits) {
        try {
            digits = getDigits(digits, text.length());
            return text.substring(0, digits);
        } catch (Exception e) {
            return text;
        }
    }

    public static String truncate(String text, int digits) {
        if (! text.contains(".")) return text;

        try {
            digits = getDigits(text.indexOf(".") + digits + 1, text.length());
            return text.substring(0, digits);
        } catch (Exception e) {
            return text;
        }
    }

    public static int getDigits(int start, int otherSize){
        return Math.min(start, otherSize);
    }

    public static ConcurrentSkipListSet<String> getCompletion(List<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    public static ConcurrentSkipListSet<String> getCompletion(ConcurrentSkipListSet<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    public static String[] argsMinus(String[] args, int... toRemove) {
        TreeMap<Integer, String> argsSet = new TreeMap<>();

        for (int i = 0; i < args.length; i++) {
            argsSet.put(i, args[i]);
        }

        for (int remove : toRemove) {
            argsSet.remove(remove);
        }

        return argsSet.values().toArray(new String[0]);
    }

    public static String argsToStringMinus(String[] args, int... toRemove){
        return normalize(argsMinus(args, toRemove));
    }

    public static String argsToString(String[] args){
        TreeMap<Integer, String> argsSet = new TreeMap<>();

        for (int i = 0; i < args.length; i++) {
            argsSet.put(i, args[i]);
        }

        return normalize(argsSet);
    }

    public static String codedString(String text){
        return formatted(newLined(SLAPI.getInstance().getMessenger().codedString(text)));
    }

    public static String replaceAmpersand(String text) {
        String regex = "((&)([0-9a-fklmnor]))";
        return text.replaceAll(regex, "§$3");
    }

    public static String formatted(String string) {
        String[] strings = string.split(" ");

        for (int i = 0; i < strings.length; i ++) {
            if (strings[i].toLowerCase(Locale.ROOT).startsWith("<to_upper>")) {
                strings[i] = strings[i].toUpperCase(Locale.ROOT).replace("<TO_UPPER>", "");
            }
            if (strings[i].toLowerCase(Locale.ROOT).startsWith("<to_lower>")) {
                strings[i] = strings[i].toLowerCase(Locale.ROOT).replace("<to_lower>", "");
            }
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < strings.length; i ++) {
            if (i == strings.length - 1) {
                builder.append(strings[i]);
            } else {
                builder.append(strings[i]).append(" ");
            }
        }

        return builder.toString();
    }

    public static String newLined(String text){
        try {
            return text.replace("%newline%", "\n")/*.replace("%uniques%", String.valueOf(StreamLine.getInstance().getPlDir().listFiles().length))*/;
        } catch (Exception e) {
            return text.replace("%newline%", "\n");
        }
    }

    public static boolean isCommand(String msg){
        return msg.startsWith("/");
    }

    public static String normalize(String[] splitMsg){
        int i = 0;
        StringBuilder text = new StringBuilder();

        for (String split : splitMsg){
            i++;
            if (split.isEmpty()) continue;

            if (i < splitMsg.length)
                text.append(split).append(" ");
            else
                text.append(split);
        }

        return text.toString();
    }

    public static String normalize(TreeSet<String> splitMsg) {
        int i = 0;
        StringBuilder text = new StringBuilder();

        for (String split : splitMsg){
            i++;
            if (split.isEmpty()) continue;

            if (i < splitMsg.size())
                text.append(split).append(" ");
            else
                text.append(split);
        }

        return text.toString();
    }

    public static String normalize(TreeMap<Integer, String> splitMsg) {
        int i = 0;
        StringBuilder text = new StringBuilder();

        for (Integer split : splitMsg.keySet()){
            i++;
            if (splitMsg.get(split).isEmpty()) continue;

            if (i < splitMsg.size())
                text.append(splitMsg.get(split)).append(" ");
            else
                text.append(splitMsg.get(split));
        }

        return text.toString();
    }

    public static boolean equalsAll(Object object, Object... toEqual){
        for (Object equal : toEqual) {
            if (! object.equals(equal)) return false;
        }

        return true;
    }

    public static boolean equalsAll(Object object, Collection<Object> toEqual){
        for (Object equal : toEqual) {
            if (! object.equals(equal)) return false;
        }

        return true;
    }

    public static boolean equalsAny(Object object, Collection<?> toEqual){
        for (Object equal : toEqual) {
            if (object.equals(equal)) return true;
        }

        return false;
    }

    public static List<String> getStringListFromString(String string) {
        String[] strings = string.split(",");

        return List.of(strings);
    }

    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        if (thingArray == null) return true;
        return thingArray.length <= lessThanOrEqualTo;
    }
}
