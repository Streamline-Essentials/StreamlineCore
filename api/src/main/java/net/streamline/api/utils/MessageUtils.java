package net.streamline.api.utils;

import com.mongodb.lang.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.AtomicString;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MessageUtils {
    public static void logInfo(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleInfoDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), GivenConfigs.getMainConfig().debugConsoleInfoPrefix() + line);
        }
    }

    public static void logWarning(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleWarningsDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), GivenConfigs.getMainConfig().debugConsoleWarningsPrefix() + line);
        }
    }

    public static void logSevere(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleErrorsDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), GivenConfigs.getMainConfig().debugConsoleErrorsPrefix() + line);
        }
    }

    public static void logDebug(String message) {
        if (GivenConfigs.getMainConfig().debugConsoleDebugDisabled()) return;
        message = message.replace("%newline%", "\n");
        for (String line : message.split("\n")) {
            SLAPI.getInstance().getMessenger().sendMessage(UserUtils.getConsole(), GivenConfigs.getMainConfig().debugConsoleDebugPrefix() + line);
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
        StreamlineUser user = UserUtils.getOrGetUser(to);

        SLAPI.getInstance().getMessenger().sendMessage(user, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        StreamlineUser user = UserUtils.getOrGetUser(to);

        SLAPI.getInstance().getMessenger().sendMessage(user, replaceAllPlayerBungee(otherUUID, message));
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<StreamlineUser, ConcurrentSkipListMap<String, String>> cache = new ConcurrentSkipListMap<>();

//    private static void cache(StreamlineUser user, String params, String outcome) {
//        ConcurrentSkipListMap<String, String> map = getCache().get(user);
//        if (map == null) map = new ConcurrentSkipListMap<>();
//        map.put(params, outcome);
//        getCache().put(user, map);
//    }
//
//    private static String getCached(StreamlineUser user, String params) {
//        ConcurrentSkipListMap<String, String> map = getCache().get(user);
//        if (map == null) {
//            map = new ConcurrentSkipListMap<>();
//            getCache().put(user, map);
//            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
//        }
//        String cached = map.get(params);
//        if (cached == null) {
//            return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
//        }
//        return cached;
//    }
//
//    private static String passResponse(StreamlineUser streamlineUser, String toParse) {
//        Map.Entry<String, StreamlinePlayer> entry = UserUtils.getOnlinePlayers().firstEntry();
//        if (entry == null) return null;
//
//        StreamlinePlayer player = entry.getValue();
//
//        CompletableFuture<Boolean> futureBool = CompletableFuture.supplyAsync(() -> {
//            ReturnableMessage message = ProxyParseMessageBuilder.build(player, toParse, streamlineUser);
//            message.registerEventCall((pm) -> cache(streamlineUser, toParse, pm.getString("parsed")));
//            return true;
//        });
//
//        if (! futureBool.join()) return null;
//        return getCached(streamlineUser, toParse);
//    }
//
//    public static String parseOnProxy(StreamlineUser streamlineUser, String toParse) {
//        if (SLAPI.getInstance().getPlatform().getServerType().equals(IStreamline.ServerType.PROXY)) return ModuleUtils.replaceAllPlayerBungee(streamlineUser, toParse);
//
//        return CompletableFuture.supplyAsync(() -> {
//            passResponse(streamlineUser, toParse);
//
//            try {
//                Thread.sleep(100);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            String r = passResponse(streamlineUser, toParse);
//
//            ConcurrentSkipListMap<String, String> map = getCache().get(streamlineUser);
//            if (map == null) return r;
//            map.remove(toParse);
//            getCache().put(streamlineUser, map);
//
//            return r;
//        }).join();
//    }

    public static String replaceAllPlayerBungee(StreamlineUser user, String of) {
        if (user == null) return of;

        return SLAPI.getRatAPI().parseAllPlaceholders(user, of).completeOnTimeout(of, 1000, TimeUnit.MILLISECONDS).join();
    }

    public static String replaceAllPlayerBungee(String uuid, String of) {
        return replaceAllPlayerBungee(UserUtils.getOrGetUser(uuid), of);
    }

    public static String parseOnProxy(StreamlineUser streamlineUser, String toParse) {
        if (SLAPI.getInstance().getPlatform().getServerType().equals(IStreamline.ServerType.PROXY))
            return ModuleUtils.replaceAllPlayerBungee(streamlineUser, toParse);

        return CompletableFuture.supplyAsync(() -> {
            StreamlinePlayer player = UserUtils.getOrGetPlayer(streamlineUser.getUuid());
            if (player == null) {
                Map.Entry<String, StreamlinePlayer> entry = UserUtils.getOnlinePlayers().firstEntry();
                if (entry == null) return null;
                player = entry.getValue();
            }

            AtomicString atomicString = new AtomicString(null);

            StreamlinePlayer finalPlayer = player;
            CompletableFuture.supplyAsync(() -> {
                ReturnableMessage message = ProxyParseMessageBuilder.build(finalPlayer, toParse, streamlineUser);
                message.registerEventCall((pm) -> {
                    atomicString.set(pm.getString("parsed"));
                });
                return true;
            }).join();

            return CompletableFuture.supplyAsync(() -> {
                String s = atomicString.get();
                while (s == null) {
                    s = atomicString.get();
                }

                return s;
            }).completeOnTimeout(null, 200, TimeUnit.MILLISECONDS).join();
        }).completeOnTimeout(null, 200, TimeUnit.MILLISECONDS).join();
    }

    public static String parseOnProxy(String toParse) {
        return parseOnProxy(UserUtils.getConsole(), toParse);
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

    public static List<String> getStringListFromString(String string) {
        String[] strings = string.split(",");

        return List.of(strings);
    }

    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        if (thingArray == null) return true;
        return thingArray.length <= lessThanOrEqualTo;
    }
}
