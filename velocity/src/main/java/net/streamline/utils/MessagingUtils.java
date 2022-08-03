package net.streamline.utils;

import com.mongodb.lang.Nullable;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.SavableConsole;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavableUser;
import net.kyori.adventure.text.TextComponent;
import net.streamline.base.configs.MainMessagesHandler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessagingUtils {
    public static void logInfo(String message) {
        sendMessage(UserManager.getConsole(), message);
    }

    public static void logWarning(String message) {
        sendMessage(UserManager.getConsole(), "&6" + message);
    }

    public static void logSevere(String message) {
        sendMessage(UserManager.getConsole(), "&c" + message);
    }

    public static String loggedModulePrefix(StreamlineModule module) {
        return "[" + module.identifier() + "] ";
    }

    public static void logInfo(StreamlineModule module, String message) {
        Streamline.getInstance().getLogger().info(loggedModulePrefix(module) + message);
    }

    public static void logWarning(StreamlineModule module, String message) {
        Streamline.getInstance().getLogger().warn(loggedModulePrefix(module) + message);
    }

    public static void logSevere(StreamlineModule module, String message) {
        Streamline.getInstance().getLogger().error(loggedModulePrefix(module) + message);
    }

    public static void sendMessage(@Nullable CommandSource to, String message) {
        if (to == null) return;
        if (Streamline.getRATAPI() == null) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    public static void sendMessage(@Nullable CommandSource to, String otherUUID, String message) {
        if (to == null) return;
        if (Streamline.getRATAPI() == null) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(otherUUID, message)));
        }
    }
    public static void sendMessage(@Nullable CommandSource to, SavableUser other, String message) {
        if (to == null) return;
        if (Streamline.getRATAPI() == null) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(other, message)));
        }
    }

    public static void sendMessage(@Nullable SavableUser to, String message) {
        if (to instanceof SavableConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.uuid), message);
    }

    public static void sendMessage(@Nullable SavableUser to, String otherUUID, String message) {
        if (to instanceof SavableConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.uuid), otherUUID, message);
    }

    public static void sendMessage(@Nullable SavableUser to, SavableUser other, String message) {
        if (to instanceof SavableConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), other, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.uuid), other, message);
    }

    public static void sendMessage(String to, String message) {
        SavableUser user = UserManager.getOrGetUser(to);

        sendMessage(user, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        SavableUser user = UserManager.getOrGetUser(to);

        sendMessage(user, replaceAllPlayerBungee(otherUUID, message));
    }

    public static void sendTitle(SavablePlayer player, StreamlineTitle title) {
        Player p = Streamline.getPlayer(player.uuid);
        if (p == null) {
            logInfo("Could not send a title to a player because player is null!");
            return;
        }

        Title t = Title.title(
                MessagingUtils.codedText(
                        MessagingUtils.replaceAllPlayerBungee(player, title.getMain())
                ),
                MessagingUtils.codedText(
                        MessagingUtils.replaceAllPlayerBungee(player, title.getSub())
                ), Title.Times.of(
                        Duration.of(title.getFadeIn() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getStay() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getFadeOut() * 50L, ChronoUnit.MILLIS)
                )
        );

        p.showTitle(t);
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

    public static TreeSet<String> getCompletion(List<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static TreeSet<String> getCompletion(TreeSet<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static String stripColor(String string){
        return PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacySection().deserialize(string))
                .replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "")
                .replaceAll("[&][1-9a-f]", "");
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

    public static TextComponent codedText(String text) {
        TextComponent tc = LegacyComponentSerializer.legacy('&').deserialize(newLined(text));

        try {
            //String ntext = text.replace(ConfigUtils.linkPre, "").replace(ConfigUtils.linkSuff, "");

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(stripColor(text));
            String foundUrl = "";

            while (matcher.find()) {
                foundUrl = matcher.group(0);

                return makeLinked(tc, foundUrl);
            }
        } catch (Exception e) {
            return tc;
        }
        return tc;
    }

    public static TextComponent clhText(String text, String hoverPrefix){
        TextComponent tc = LegacyComponentSerializer.legacy('&').deserialize(newLined(text));

        try {
            //String ntext = text.replace(ConfigUtils.linkPre, "").replace(ConfigUtils.linkSuff, "");

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(stripColor(text));
            String foundUrl = "";

            while (matcher.find()) {
                foundUrl = matcher.group(0);
                return makeHoverable(makeLinked(tc, foundUrl), hoverPrefix + foundUrl);
            }
        } catch (Exception e) {
            return tc;
        }
        return tc;
    }

    public static String codedString(String text){
        return formatted(newLined(text)).replace("&", "§");
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

    public static TextComponent makeLinked(String text, String url){
        return Component.text(text).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url));
    }

    public static TextComponent makeLinked(TextComponent textComponent, String url){
        return textComponent.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url));
    }

    public static TextComponent makeHoverable(String text, String hoverText){
        return Component.text(text).hoverEvent(HoverEvent.showText(codedText(hoverText)));
    }

    public static TextComponent makeHoverable(TextComponent textComponent, String hoverText){
        return textComponent.hoverEvent(HoverEvent.showText(codedText(hoverText)));
    }

    public static String isolateChatColor(String format) {
        String[] strings = format.split(" ");

        for (String string : strings) {
            if (string.contains("%message%")) {
                String[] gotten = string.split("%message%");
                return gotten[0];
            }
        }

        return "";
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

    public static String replaceAllPlayerBungee(SavableUser user, String of) {
        if (user == null) return of;

        return Streamline.getRATAPI().parseAllPlaceholders(user, of);
    }

    public static String replaceAllPlayerBungee(String uuid, String of) {
        return replaceAllPlayerBungee(UserManager.getOrGetUser(uuid), of);
    }

    public static String replaceAllPlayerBungee(CommandSource sender, String of) {
        return replaceAllPlayerBungee(UserManager.getOrGetUser(sender), of);
    }

    public static Collection<ServerInfo> getServers() {
        Collection<ServerInfo> r = new ArrayList<>();
        Streamline.getInstance().getProxy().getAllServers().forEach(a -> r.add(a.getServerInfo()));
        return r;
    }

    public static boolean equalsAnyServer(String servername) {
        for (ServerInfo serverInfo : getServers()) {
            if (serverInfo.getName().equals(servername)) return true;
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
