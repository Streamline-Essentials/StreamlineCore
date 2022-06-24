package net.streamline.utils;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.mongodb.lang.Nullable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.base.Streamline;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavableUser;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MessagingUtils {
    public static String loggedMessage(String message) {
        return Streamline.getInstance().getName() + " > " + message;
    }

    public static void logInfo(String message) {
        Streamline.getInstance().getLogger().info(loggedMessage(message));
    }

    public static void logWarning(String message) {
        Streamline.getInstance().getLogger().warning(loggedMessage(message));
    }

    public static void logSevere(String message) {
        Streamline.getInstance().getLogger().severe(loggedMessage(message));
    }

    public static void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        to.sendMessage(codedText(message));
    }

    public static void sendMessage(@Nullable SavableUser user, String message) {
        if (user == null) return;
        user.sendMessage(message);
    }

    public static void sendMessage(String uuid, String message) {
        SavableUser user = UserManager.getOrGetUser(uuid);

        sendMessage(user, message);
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

    public static TextComponent hexedText(String text) {
        text = codedString(text);

        try {
            //String ntext = text.replace(ConfigUtils.linkPre(), "").replace(ConfigUtils.linkSuff, "");

            Pattern pattern = Pattern.compile("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(stripColor(text));
            String found = "";

            String textLeft = text;

            TextComponent tc = new TextComponent();

            int i = 0;
            boolean find = false;
            TreeMap<Integer, String> founds = new TreeMap<>();

            while (matcher.find()) {
                find = true;
                found = matcher.group(0);

                founds.put(i, found);

                i ++;
            }
            if (! find) return new TextComponent(text);

            TreeMap<Integer, String> pieces = new TreeMap<>();
            int iter = 0;
            int from = 0;
            for (Integer key : founds.keySet()) {
                int at = text.indexOf(founds.get(key), from);
                pieces.put(iter, text.substring(at));
                from = at;
                iter ++;
            }

            tc = new TextComponent(pieces.get(0));

            for (Integer key : pieces.keySet()) {
                if (key == 0) continue;

                String p = pieces.get(key);
                String f = p.substring(0, "<#123456>".length());

                String colorHex = f.substring(1, f.indexOf('>'));
                String after = p.substring(f.length());

//                tc.addExtra(new LiteralText(after).styled(style -> style.withColor(Integer.decode(colorHex))));
                BaseComponent[] bc = new ComponentBuilder(after).color(ChatColor.of(Color.decode(colorHex))).create();

                for (BaseComponent b : bc) {
                    tc.addExtra(b);
                }
            }

            return tc;
        } catch (Exception e) {
            e.printStackTrace();
            return new TextComponent(text);
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
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
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
        text = ChatColor.translateAlternateColorCodes('&', newLined(text));

        try {
            //String ntext = text.replace(ConfigUtils.linkPre(), "").replace(ConfigUtils.linkSuff, "");

            Pattern pattern = Pattern.compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(stripColor(text));
            String foundUrl = "";

            while (matcher.find()) {
                foundUrl = matcher.group(0);

                return makeLinked(text, foundUrl);
            }
        } catch (Exception e) {
            return hexedText(text);
        }
        return hexedText(text);
    }

    public static TextComponent clhText(String text, String hoverPrefix){
        text = ChatColor.translateAlternateColorCodes('&', newLined(text));

        try {
            //String ntext = text.replace(ConfigUtils.linkPre(), "").replace(ConfigUtils.linkSuff, "");

            Pattern pattern = Pattern.compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(stripColor(text));
            String foundUrl = "";

            while (matcher.find()) {
                foundUrl = matcher.group(0);

                TextComponent tc = makeLinked(text, foundUrl);
                return makeHoverable(tc, hoverPrefix + foundUrl);
            }
        } catch (Exception e) {
            return hexedText(text);
        }
        return hexedText(text);
    }

    public static String codedString(String text){
        return ChatColor.translateAlternateColorCodes('&', formatted(newLined(text)));
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
        TextComponent tc = hexedText(text);
        ClickEvent ce = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        tc.setClickEvent(ce);
        return tc;
    }

    public static TextComponent makeHoverable(String text, String hoverText){
        TextComponent tc = new TextComponent(text);
        HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(codedString(hoverText)));
        tc.setHoverEvent(he);
        return tc;
    }

    public static TextComponent makeHoverable(TextComponent textComponent, String hoverText){
        HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(codedString(hoverText)));
        textComponent.setHoverEvent(he);
        return textComponent;
    }

    public static TreeMap<Integer, ProxiedPlayer> getTaggedPlayersIndexed(String[] args, String serverName) {
        TreeMap<Integer, ProxiedPlayer> toIndex = new TreeMap<>();
        List<ProxiedPlayer> players = Streamline.getInstance().playersOnServer(serverName);

        for (ProxiedPlayer player : players) {
            for (int i = 0; i < args.length; i ++) {
                if (player.getName().equals(args[i])) {
                    toIndex.put(i, player);
                }
            }
        }

        return toIndex;
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

    public static String replaceAllPlayerBungee(String of, SavableUser user) {
        if (user == null) return of;

        return Streamline.getRATAPI().parseAllPlaceholders(user, of);
    }

    public static String replaceAllPlayerBungee(String of, String uuid) {
        return replaceAllPlayerBungee(of, UserManager.getOrGetUser(uuid));
    }

    public static String replaceAllPlayerBungee(String of, CommandSender sender) {
        return replaceAllPlayerBungee(of, UserManager.getOrGetUser(sender));
    }

    public static Collection<ServerInfo> getServers() {
        return Streamline.getInstance().getProxy().getServers().values();
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
