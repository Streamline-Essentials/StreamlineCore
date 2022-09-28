package net.streamline.platform;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.mongodb.lang.Nullable;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public void logInfo(String message) {
        sendMessage(UserUtils.getConsole(), message);
    }

    public void logWarning(String message) {
        sendMessage(UserUtils.getConsole(), "&6" + message);
    }

    public void logSevere(String message) {
        sendMessage(UserUtils.getConsole(), "&c" + message);
    }

    public String loggedModulePrefix(StreamlineModule module) {
        return "[" + module.identifier() + "] ";
    }

    public void logInfo(StreamlineModule module, String message) {
        Streamline.getInstance().getLogger().info(loggedModulePrefix(module) + message);
    }

    public void logWarning(StreamlineModule module, String message) {
        Streamline.getInstance().getLogger().warning(loggedModulePrefix(module) + message);
    }

    public void logSevere(StreamlineModule module, String message) {
        Streamline.getInstance().getLogger().severe(loggedModulePrefix(module) + message);
    }

    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        if (SLAPI.getRatAPI() == null) {
            to.sendMessage(codedString(message));
        } else {
            to.sendMessage(codedString(replaceAllPlayerBungee(to, message)));
        }
    }

    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        if (SLAPI.getRatAPI() == null) {
            to.sendMessage(codedString(message));
        } else {
            to.sendMessage(codedString(replaceAllPlayerBungee(otherUUID, message)));
        }
    }
    public void sendMessage(@Nullable CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;
        if (SLAPI.getRatAPI() == null) {
            to.sendMessage(codedString(message));
        } else {
            to.sendMessage(codedString(replaceAllPlayerBungee(other, message)));
        }
    }

    public void sendMessage(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsole(), message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsole(), otherUUID, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsole(), other, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), other, message);
    }

    public void sendMessage(String to, String message) {
        StreamlineUser user = UserUtils.getOrGetUser(to);

        sendMessage(user, message);
    }

    public void sendMessage(@Nullable String to, String otherUUID, String message) {
        StreamlineUser user = UserUtils.getOrGetUser(to);

        sendMessage(user, replaceAllPlayerBungee(otherUUID, message));
    }

    public void sendTitle(StreamlinePlayer player, StreamlineTitle title) {
        ProxiedPlayer p = Streamline.getPlayer(player.getUuid());
        if (p == null) {
            logInfo("Could not send a title to a player because player is null!");
            return;
        }

        ;

        p.sendTitle(Streamline.getInstance().getProxy().createTitle()
                .title(codedText(title.getMain()))
                .subTitle(codedText(title.getSub()))
                .fadeIn((int) title.getFadeIn())
                .stay((int) title.getStay())
                .fadeOut((int) title.getFadeOut())
        );
    }

    public String getListAsFormattedString(List<?> list) {
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

    public String removeExtraDot(String string){
        String s = string.replace("..", ".");

        if (s.endsWith(".")) {
            s = s.substring(0, s.lastIndexOf('.'));
        }

        return s;
    }

    public String resize(String text, int digits) {
        try {
            digits = getDigits(digits, text.length());
            return text.substring(0, digits);
        } catch (Exception e) {
            return text;
        }
    }

    public String truncate(String text, int digits) {
        if (! text.contains(".")) return text;

        try {
            digits = getDigits(text.indexOf(".") + digits + 1, text.length());
            return text.substring(0, digits);
        } catch (Exception e) {
            return text;
        }
    }

    public int getDigits(int start, int otherSize){
        if (start <= otherSize) {
            return start;
        } else {
            return otherSize;
        }
    }

    public TextComponent hexedText(String text) {
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

    public TreeSet<String> getCompletion(List<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public TreeSet<String> getCompletion(TreeSet<String> of, String param){
        return of.stream()
                .filter(completion -> completion.toLowerCase(Locale.ROOT).startsWith(param.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
    }

    public String[] argsMinus(String[] args, int... toRemove) {
        TreeMap<Integer, String> argsSet = new TreeMap<>();

        for (int i = 0; i < args.length; i++) {
            argsSet.put(i, args[i]);
        }

        for (int remove : toRemove) {
            argsSet.remove(remove);
        }

        return argsSet.values().toArray(new String[0]);
    }

    public String argsToStringMinus(String[] args, int... toRemove){
        return normalize(argsMinus(args, toRemove));
    }

    public String argsToString(String[] args){
        TreeMap<Integer, String> argsSet = new TreeMap<>();

        for (int i = 0; i < args.length; i++) {
            argsSet.put(i, args[i]);
        }

        return normalize(argsSet);
    }

    public TextComponent codedText(String text) {
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

    public TextComponent clhText(String text, String hoverPrefix){
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

    public String codedString(String text){
        return ChatColor.translateAlternateColorCodes('&', formatted(newLined(text)));
    }

    public String formatted(String string) {
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

    public TextComponent makeLinked(String text, String url){
        TextComponent tc = hexedText(text);
        ClickEvent ce = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        tc.setClickEvent(ce);
        return tc;
    }

    public TextComponent makeHoverable(String text, String hoverText){
        TextComponent tc = new TextComponent(text);
        HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(codedString(hoverText)));
        tc.setHoverEvent(he);
        return tc;
    }

    public TextComponent makeHoverable(TextComponent textComponent, String hoverText){
        HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(codedString(hoverText)));
        textComponent.setHoverEvent(he);
        return textComponent;
    }

    public TreeMap<Integer, ProxiedPlayer> getTaggedPlayersIndexed(String[] args, String serverName) {
        TreeMap<Integer, ProxiedPlayer> toIndex = new TreeMap<>();
        List<ProxiedPlayer> players = BasePlugin.playersOnServer(serverName);

        for (ProxiedPlayer player : players) {
            for (int i = 0; i < args.length; i ++) {
                if (player.getName().equals(args[i])) {
                    toIndex.put(i, player);
                }
            }
        }

        return toIndex;
    }

    public String isolateChatColor(String format) {
        String[] strings = format.split(" ");

        for (String string : strings) {
            if (string.contains("%message%")) {
                String[] gotten = string.split("%message%");
                return gotten[0];
            }
        }

        return "";
    }

    public String newLined(String text){
        try {
            return text.replace("%newline%", "\n")/*.replace("%uniques%", String.valueOf(StreamLine.getInstance().getPlDir().listFiles().length))*/;
        } catch (Exception e) {
            return text.replace("%newline%", "\n");
        }
    }

    public boolean isCommand(String msg){
        return msg.startsWith("/");
    }

    public String normalize(String[] splitMsg){
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

    public String normalize(TreeSet<String> splitMsg) {
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

    public String normalize(TreeMap<Integer, String> splitMsg) {
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

    public boolean equalsAll(Object object, Object... toEqual){
        for (Object equal : toEqual) {
            if (! object.equals(equal)) return false;
        }

        return true;
    }

    public boolean equalsAll(Object object, Collection<Object> toEqual){
        for (Object equal : toEqual) {
            if (! object.equals(equal)) return false;
        }

        return true;
    }

    public boolean equalsAny(Object object, Collection<?> toEqual){
        for (Object equal : toEqual) {
            if (object.equals(equal)) return true;
        }

        return false;
    }

    public String replaceAllPlayerBungee(StreamlineUser user, String of) {
        if (user == null) return of;

        return SLAPI.getRatAPI().parseAllPlaceholders(user, of).join();
    }

    public String replaceAllPlayerBungee(String uuid, String of) {
        return replaceAllPlayerBungee(UserUtils.getOrGetUser(uuid), of);
    }

    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        return replaceAllPlayerBungee(UserUtils.getOrGetUserByName(sender.getName()), of);
    }

    public List<String> getStringListFromString(String string) {
        String[] strings = string.split(",");

        return List.of(strings);
    }

    public boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        if (thingArray == null) return true;
        return thingArray.length <= lessThanOrEqualTo;
    }
}
