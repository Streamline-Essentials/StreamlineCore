package net.streamline.platform;

import net.md_5.bungee.api.chat.hover.content.Entity;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.chat.ComponentSerializer;
import net.streamline.api.modules.ModuleUtils;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.text.HexResulter;
import net.streamline.api.text.TextManager;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import org.jetbrains.annotations.Nullable;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.thebase.lib.re2j.Pattern;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class Messenger implements IMessenger {
    @Getter
    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public void sendMessage(@Nullable CommandSender to, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(replaceAllPlayerBungee(to, message)));
        }
    }

    public void sendMessage(@Nullable CommandSender to, String otherUUID, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(otherUUID, message)));
        }
    }
    public void sendMessage(@Nullable CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;
        if (! SLAPI.isReady()) {
            to.sendMessage(codedText(message));
        } else {
            to.sendMessage(codedText(MessageUtils.replaceAllPlayerBungee(other, message)));
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

    public void sendMessageRaw(CommandSender to, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(replaceAllPlayerBungee(to, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSender to, String otherUUID, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(otherUUID, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(CommandSender to, StreamlineUser other, String message) {
        if (to == null) return;

        BaseComponent[] component;
        if (! SLAPI.isReady()) {
            component = new ComponentBuilder(message).create();
        } else {
            component = new ComponentBuilder(MessageUtils.replaceAllPlayerBungee(other, message)).create();
        }

        to.sendMessage(component);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsole(), message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, String otherUUID, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsole(), otherUUID, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), otherUUID, message);
    }

    public void sendMessageRaw(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        if (to instanceof StreamlineConsole) sendMessageRaw(Streamline.getInstance().getProxy().getConsole(), other, message);
        if (to == null) return;
        sendMessageRaw(Streamline.getPlayer(to.getUuid()), other, message);
    }

    public void sendTitle(StreamlinePlayer player, StreamlineTitle title) {
        ProxiedPlayer p = Streamline.getPlayer(player.getUuid());
        if (p == null) {
            MessageUtils.logInfo("Could not send a title to a player because player is null!");
            return;
        }

        p.sendTitle(Streamline.getInstance().getProxy().createTitle()
                .title(codedText(title.getMain()))
                .subTitle(codedText(title.getSub()))
                .fadeIn((int) title.getFadeIn())
                .stay((int) title.getStay())
                .fadeOut((int) title.getFadeOut())
        );
    }

    @Override
    public String codedString(String from) {
        return ChatColor.translateAlternateColorCodes('&', ModuleUtils.newLined(from));
    }

    public String stripColor(String string){
        return ChatColor.stripColor(string).replaceAll("([<][#][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][1-9a-f][>])+", "");
    }

    public static String replaceHex(String text) {
        for (HexResulter resulter : TextManager.getHexResulters()) {
            Pattern pattern = Pattern.compile(Pattern.quote(resulter.getStarter()) + "([0-9a-fA-F]{6})" + Pattern.quote(resulter.getEnder()));
            Matcher matcher = pattern.matcher(text);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String colorCode = matcher.group(1);
                String minecraftColor = ChatColor.of("#" + colorCode).toString();
                matcher.appendReplacement(sb, minecraftColor);
            }

            matcher.appendTail(sb);
            text = sb.toString();
        }

        return text;
    }

    public BaseComponent[] codedText(String from) {
        String raw = codedString(from);
        raw = replaceHex(raw);

        List<BaseComponent> componentsList = new ArrayList<>();

        Pattern pattern = Pattern.compile("!!json:(\\{[^}]*\\})");
        Matcher matcher = pattern.matcher(from);

        int lastEnd = 0;

        while (matcher.find()) {
            String before = from.substring(lastEnd, matcher.start());
            String jsonStr = matcher.group(1);

            BaseComponent[] beforeComponent = TextComponent.fromLegacyText(MessageUtils.codedString(before));
            BaseComponent[] jsonComponent = ComponentSerializer.parse(jsonStr);

            for (BaseComponent component : beforeComponent) {
                componentsList.add(component);
            }
            for (BaseComponent component : jsonComponent) {
                componentsList.add(component);
            }

            lastEnd = matcher.end();
        }

        // Append any remaining text after the last JSON block.
        if (lastEnd < from.length()) {
            BaseComponent[] remainingComponent = TextComponent.fromLegacyText(MessageUtils.codedString(raw.substring(lastEnd)));
            for (BaseComponent component : remainingComponent) {
                componentsList.add(component);
            }
        }

        return componentsList.toArray(new BaseComponent[0]);
    }

    public String replaceAllPlayerBungee(CommandSender sender, String of) {
        return MessageUtils.replaceAllPlayerBungee(UserUtils.getOrGetUserByName(sender.getName()), of);
    }
}
