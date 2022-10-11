package net.streamline.api.utils;

import com.mongodb.lang.Nullable;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.modules.StreamlineSpringModule;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class MessageUtils {
    public static void logInfo(String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), "&f[&3StreamlineCore&f] &r" + line);
        }
    }

    public static void logWarning(String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), "&f[&3StreamlineCore&f] &6" + line);
        }
    }

    public static void logSevere(String message) {
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), "&f[&3StreamlineCore&f] &c" + line);
        }
    }

    public static String loggedModulePrefix(ModuleLike module) {
        return "[" + module.identifier() + "] ";
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

    public static void sendMessage(String to, String message) {
        StreamlineUser user = UserUtils.getOrGetUser(to);

        SLAPI.getInstance().getMessenger().sendMessage(user, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        StreamlineUser user = UserUtils.getOrGetUser(to);

        SLAPI.getInstance().getMessenger().sendMessage(user, replaceAllPlayerBungee(otherUUID, message));
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
        if (start <= otherSize) {
            return start;
        } else {
            return otherSize;
        }
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
        return formatted(newLined(SLAPI.getInstance().getMessenger().codedString(text))).replace('&', '\u00a7');
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
            if (split.equals("")) continue;

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
            if (split.equals("")) continue;

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
            if (splitMsg.get(split).equals("")) continue;

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

    public static String replaceAllPlayerBungee(StreamlineUser user, String of) {
        if (user == null) return of;

        return SLAPI.getRatAPI().parseAllPlaceholders(user, of).join();
    }

    public static String replaceAllPlayerBungee(String uuid, String of) {
        return replaceAllPlayerBungee(UserUtils.getOrGetUser(uuid), of);
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
