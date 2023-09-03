package net.streamline.platform;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.streamline.api.modules.ModuleUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.text.HexResulter;
import net.streamline.api.text.TextManager;
import net.streamline.api.utils.MessageUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import org.jetbrains.annotations.Nullable;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.thebase.lib.re2j.Pattern;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public void sendMessage(@Nullable CommandSource to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    public void sendMessage(@Nullable CommandSource to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }
    public void sendMessage(@Nullable CommandSource to, StreamlineUser other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
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

    public void sendMessageRaw(CommandSource to, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(replaceAllPlayerBungee(to, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSource to, String otherUUID, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(otherUUID, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSource to, StreamlineUser other, String message) {
        if (to == null) return;

        Component component;
        if (! SLAPI.isReady()) {
            component = Component.text(message);
        } else {
            component = Component.text(MessageUtils.replaceAllPlayerBungee(other, message));
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsoleCommandSource(), message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsoleCommandSource(), otherUUID, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsoleCommandSource(), other, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), other, message);
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
        return ModuleUtils.newLined(from.replace("&", "§"));
    }

    public String stripColor(String string){
        return PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacySection().deserialize(string))
                .replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "")
                .replaceAll("[&][1-9a-f]", "");
    }

    public String asString(Component textComponent){
        return LegacyComponentSerializer.legacySection().serialize(textComponent);
    }

    public static Component legacyCode(String from) {
        return LegacyComponentSerializer.builder().extractUrls().character('&').hexColors().build().deserialize(from);
    }

    public static String replaceHex(String text) {
        for (HexResulter resulter : TextManager.getHexResulters()) {
            Pattern pattern = Pattern.compile(Pattern.quote(resulter.getStarter()) + "([0-9a-fA-F]{6})" + Pattern.quote(resulter.getEnder()));
            Matcher matcher = pattern.matcher(text);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String colorCode = matcher.group(1);
                // Parse the hex string to an RGB integer
                int rgb = Integer.parseInt(colorCode, 16);
                // Convert the RGB integer to a Component
                String minecraftColor = LegacyComponentSerializer.legacyAmpersand().serialize(Component.text("", TextColor.color(rgb)));
                // Replace the hex code in the text
                matcher.appendReplacement(sb, minecraftColor);
            }

            matcher.appendTail(sb);
            text = sb.toString();
        }

        return text;
    }

    public Component codedText(String from) {
        String raw = codedString(from); // Assuming codedString is another method you've implemented
        raw = replaceHex(raw);

        List<Component> componentsList = new ArrayList<>();

        Pattern pattern = Pattern.compile("!!json:(\\{[^}]*\\})");
        Matcher matcher = pattern.matcher(from);

        int lastEnd = 0;

        while (matcher.find()) {
            String before = from.substring(lastEnd, matcher.start());
            String jsonStr = matcher.group(1);

            Component beforeComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(MessageUtils.codedString(before));

            // Use Adventure's JSON deserializer here if GsonComponentSerializer is not available
            Component jsonComponent = JSONComponentSerializer.json().deserialize(jsonStr);

            componentsList.add(beforeComponent);
            componentsList.add(jsonComponent);

            lastEnd = matcher.end();
        }

        // Append any remaining text after the last JSON block
        if (lastEnd < from.length()) {
            Component remainingComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(MessageUtils.codedString(from.substring(lastEnd)));
            componentsList.add(remainingComponent);
        }

        return Component.empty().children(componentsList);
    }

    public String replaceAllPlayerBungee(CommandSource sender, String of) {
        return MessageUtils.replaceAllPlayerBungee(UserManager.getInstance().getOrGetUser(sender), of);
    }
}
