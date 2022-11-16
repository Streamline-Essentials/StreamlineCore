package net.streamline.platform;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public void sendMessage(@Nullable CommandSource to, String message) {
        if (to == null) return;
        if (SLAPI.getRatAPI() == null) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    public void sendMessage(@Nullable CommandSource to, String otherUUID, String message) {
        if (to == null) return;
        if (SLAPI.getRatAPI() == null) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }
    public void sendMessage(@Nullable CommandSource to, StreamlineUser other, String message) {
        if (to == null) return;
        if (SLAPI.getRatAPI() == null) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
        }
    }

    public void sendMessage(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessage(Streamline.getInstance().getProxy().getConsoleCommandSource(), other, message);
        if (to == null) return;
        sendMessage(Streamline.getPlayer(to.getUuid()), other, message);
    }

    @Override
    public void sendTitle(StreamlinePlayer player, StreamlineTitle title) {
        Player p = Streamline.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        Title t = Title.title(
                codedText(
                        MessageUtils.replaceAllPlayerBungee(player, title.getMain())
                ),
                codedText(
                        MessageUtils.replaceAllPlayerBungee(player, title.getSub())
                ), Title.Times.times(
                        Duration.of(title.getFadeIn() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getStay() * 50L, ChronoUnit.MILLIS),
                        Duration.of(title.getFadeOut() * 50L, ChronoUnit.MILLIS)
                )
        );

        p.showTitle(t);
    }

    @Override
    public String codedString(String from) {
        return from.replace('&', '\u00a7');
    }

    public String stripColor(String string){
        return PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacySection().deserialize(string))
                .replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "")
                .replaceAll("[&][1-9a-f]", "");
    }

    public TextComponent codedText(String text) {
        TextComponent tc = LegacyComponentSerializer.legacy('&').deserialize(MessageUtils.newLined(text));

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

    public TextComponent clhText(String text, String hoverPrefix){
        TextComponent tc = LegacyComponentSerializer.legacy('&').deserialize(MessageUtils.newLined(text));

        try {
            //String ntext = text.replace(ConfigUtils.linkPre, "").replace(ConfigUtils.linkSuff, "");

            Pattern pattern = Pattern.compile("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", Pattern.CASE_INSENSITIVE);
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

    public TextComponent makeLinked(String text, String url){
        return Component.text(text).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url));
    }

    public TextComponent makeLinked(TextComponent textComponent, String url){
        return textComponent.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url));
    }

    public TextComponent makeHoverable(String text, String hoverText){
        return Component.text(text).hoverEvent(HoverEvent.showText(codedText(hoverText)));
    }

    public TextComponent makeHoverable(TextComponent textComponent, String hoverText){
        return textComponent.hoverEvent(HoverEvent.showText(codedText(hoverText)));
    }

    public TreeMap<Integer, Player> getTaggedPlayersIndexed(String[] args, String serverName) {
        TreeMap<Integer, Player> toIndex = new TreeMap<>();
        List<Player> players = BasePlugin.playersOnServer(serverName);

        for (Player player : players) {
            for (int i = 0; i < args.length; i ++) {
                if (player.getUsername().equals(args[i])) {
                    toIndex.put(i, player);
                }
            }
        }

        return toIndex;
    }

    public String replaceAllPlayerBungee(CommandSource sender, String of) {
        return MessageUtils.replaceAllPlayerBungee(UserManager.getInstance().getOrGetUser(sender), of);
    }
}
